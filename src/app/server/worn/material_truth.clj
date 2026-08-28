(ns app.server.worn.material-truth
  "Reads over facet-masters: served instances, the blast radius of a pointer move, announcements, case reports, and the world at a cut.
   Takes: an object-container runtime, a facet-master spec, a subject id, and an optional cut {master-id → revision-id}.
   Gives: plain maps under :facet-master/*, :blast/*, :announce/*, :case/*, :history/*; writes only through facet-master.
   Holds nothing."
  (:require [app.server.episode.episode :as episode]
            [app.server.worn.facet-master :as facet-master]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.worn.activation-event :as activation-event]
            [app.server.worn.facet-engine :as facet-engine]
            [app.server.worn.facet-masters :as facet-masters]))

;; ===========================================================================
;; Write paths — a deviation and its index, in that order
;; ===========================================================================

(defn- register!
  "Index upkeep AFTER the deviation has landed. Best-effort and deliberately
   unable to fail the write: the index is not the truth (T7), so a failure here
   costs discoverability, never correctness."
  [oc-rt parent-spec subject-uid result opts]
  (when (:accepted? result)
    (try
      (episode/register-instance-masters!
       oc-rt
       {:conversation-id (:conversation-id opts)
        :time-ms (:time-ms opts)
        :entries [{:facet (:facet-master/facet parent-spec)
                   :subject subject-uid
                   :instance-master-id (:instance-master-id result)
                   :parent-id (facet-master/master-id parent-spec)}]})
      (catch Throwable t
        {:status :index-failed :error (.getMessage t)})))
  result)

(defn deviate!
  [oc-rt parent-spec subject-uid overrides opts]
  (register! oc-rt parent-spec subject-uid
             (facet-master/deviate! oc-rt parent-spec subject-uid
                                    overrides opts)
             opts))

(defn release-deviation!
  [oc-rt parent-spec subject-uid opts]
  (register! oc-rt parent-spec subject-uid
             (facet-master/release-deviation! oc-rt parent-spec subject-uid opts)
             opts))

(defn pin!
  [oc-rt parent-spec subject-uid pinned-revision-id opts]
  (register! oc-rt parent-spec subject-uid
             (facet-master/pin! oc-rt parent-spec subject-uid
                                pinned-revision-id opts)
             opts))

(defn unpin!
  [oc-rt parent-spec subject-uid opts]
  (register! oc-rt parent-spec subject-uid
             (facet-master/unpin! oc-rt parent-spec subject-uid opts)
             opts))

(defn rebuild-instance-registry!
  "T7 / G13 — rebuild the index from CONTAINER EXISTENCE, which is the truth.

   The index is a convenience: it tells the serve where to look. This function
   is the proof that it can never become authoritative — drop every row, probe
   the candidate subjects, and the same rows come back. `subjects` is the
   candidate set (the world's units); probing is a repair path, not a hot one."
  [oc-rt {:keys [conversation-id subjects time-ms]}]
  (let [entries (vec
                 (for [spec facet-masters/specs
                       s (or subjects [])
                       :let [st (facet-master/instance-state oc-rt spec s)]
                       :when (:exists? st)]
                   {:facet (:facet-master/facet spec)
                    :subject s
                    :instance-master-id (:instance-master-id st)
                    :parent-id (facet-master/master-id spec)}))]
    (assoc (episode/register-instance-masters!
            oc-rt {:conversation-id conversation-id
                   :entries entries
                   :time-ms time-ms})
           :rebuilt entries)))

;; ===========================================================================
;; The served instance tier — ONE batched read beside the facet-materials serve
;; ===========================================================================

(defn- compiled-pin-target
  "The PINNED shared revision, compiled under ITS OWN grammar. Resolving it
   here means the client never issues a read to answer `what am I pinned to`
   (T5 — the echo bar dies if wear resolution costs a read per block). A
   missing or malformed target compiles invalid and the wear law floors it."
  [oc-rt parent-spec pinned-revision-id]
  (when pinned-revision-id
    (let [rev (ocr/read-revision oc-rt pinned-revision-id)]
      (if (nil? rev)
        {:valid? false :revision-id pinned-revision-id
         :errors [{:type :facet-master/pinned-revision-missing}]}
        (assoc (facet-engine/compile-source parent-spec (:content-text rev))
               :revision-id pinned-revision-id)))))

(defn served-instance
  "One subject's instance master in the shape the shared wear law consumes.
   nil when the subject has no instance master — the overwhelmingly common
   case, and the one that must cost nothing."
  [oc-rt parent-spec subject-uid]
  (let [{:keys [exists? compiled state instance-master-id pin holds]}
        (facet-master/instance-state oc-rt parent-spec subject-uid)]
    (when exists?
      (let [active-id (get-in state [:active-revision :revision-id])
            latest-id (get-in state [:latest-revision :revision-id])]
        {:facet-master/id instance-master-id
         :facet-master/parent-id (facet-master/master-id parent-spec)
         :facet-master/facet (:facet-master/facet parent-spec)
         :facet-master/subject subject-uid
         :facet-master/grammar (:grammar compiled)
         :facet-master/material (:material compiled)
         :facet-master/valid? (true? (:valid? compiled))
         :facet-master/errors (vec (:errors compiled))
         :facet-master/active-revision-id active-id
         :facet-master/latest-revision-id latest-id
         ;; latest ≠ active, enforced everywhere: a subject can hold an
         ;; unactivated candidate deviation exactly like a shared master can
         :facet-master/candidate? (and (some? latest-id)
                                       (not= latest-id active-id))
         :facet-master/holds holds
         :facet-master/pin pin
         :facet-master/pinned
         (compiled-pin-target oc-rt parent-spec
                              (:pinned-revision-id pin))}))))

(defn served-instances
  "{facet → {subject → served-instance}} for the registered deviant subjects.

   The registry (ONE read) supplies the subject list so this never enumerates
   blind; container existence is still the truth, so a subject passed in
   explicitly is honored even when the index has not caught up."
  ([oc-rt] (served-instances oc-rt nil))
  ([oc-rt {:keys [conversation-id extra-subjects]}]
   (let [object-key (episode/episode-object-key
                     (or conversation-id episode/genesis-conversation-id))
         indexed (try (episode/read-instance-registry oc-rt object-key)
                      (catch Throwable _ []))
         pairs (into (into #{} (map (juxt :facet :subject)) indexed)
                     (for [spec facet-masters/specs
                           s (or extra-subjects [])]
                       [(:facet-master/facet spec) s]))]
     (reduce
      (fn [acc [facet subject]]
        (let [spec (first (filter #(= facet (:facet-master/facet %))
                                  facet-masters/specs))
              served (when spec (served-instance oc-rt spec subject))]
          (cond-> acc
            served (assoc-in [facet subject] served))))
      {}
      (sort-by (juxt (comp str first) (comp str second)) pairs)))))

;; ===========================================================================
;; Blast radius — what ONE pointer flip will be felt by, before the flip
;; ===========================================================================

(defn blast-radius
  "The exact wearer set a scoped activation will reach, derived BEFORE the
   flip and WITHOUT writing anything (T1).

   `candidate-wearers` is the set of subjects currently wearing the facet — the
   served page, typically. Subjects that will NOT move are listed with the
   reason, because `47 blocks will change` is a different claim from
   `47 blocks will change, 2 are pinned and 1 has its own deviation`.

   `:blast/includes-future-wearers?` is said out loud and is always true for
   `:scope/all-unpinned`: the pointer is what is worn, so a subject born after
   this activation wears the new revision without anything being copied to it.
   That is a promise the projection can make honestly precisely because no
   fan-out ever happens."
  [oc-rt parent-spec {:keys [scope candidate-wearers conversation-id]}]
  (let [scope (or scope (activation-event/all-unpinned-scope))
        facet (:facet-master/facet parent-spec)
        instances (get (served-instances
                        oc-rt {:conversation-id conversation-id
                               :extra-subjects candidate-wearers})
                       facet)
        wearers (vec (sort (set (or candidate-wearers []))))
        classify (fn [subject]
                   (let [i (get instances subject)]
                     (cond
                       (nil? i) :will-move
                       (not (:facet-master/valid? i)) :will-move
                       (= :holds/pin (:facet-master/holds i)) :pinned
                       (= :holds/deviation (:facet-master/holds i)) :deviating
                       :else :will-move)))
        grouped (group-by classify wearers)]
    (case (first scope)
      :scope/subject
      (let [s (second scope)]
        {:blast/master-id (facet-master/master-id parent-spec)
         :blast/facet facet
         :blast/scope scope
         :blast/will-move (if (contains? (set wearers) s) [s] [])
         :blast/pinned []
         :blast/deviating []
         :blast/includes-future-wearers? false
         :blast/counted-over (count wearers)})

      {:blast/master-id (facet-master/master-id parent-spec)
       :blast/facet facet
       :blast/scope scope
       :blast/will-move (vec (get grouped :will-move []))
       :blast/pinned (vec (get grouped :pinned []))
       :blast/deviating (vec (get grouped :deviating []))
       ;; by REFERENCE — the pointer is the truth, so wearers that do not
       ;; exist yet are covered without a single write reaching them
       :blast/includes-future-wearers? true
       :blast/counted-over (count wearers)})))

;; ===========================================================================
;; Announcements — one activation, three scales, all distinguishable
;; ===========================================================================

(def change-kinds
  "The distinguishable shapes a change can take (deliverable 5). `:preview` is
   here so the vocabulary can SAY it; the server never mints one, because a
   preview is by definition something that did not happen."
  #{:change/preview
    :change/deviation
    :change/pin
    :change/unpin
    :change/scoped-activation
    :change/canonical-activation
    :change/rollback
    :change/recovery})

(defn change-kind
  "Classify one activation from its EVENT, not from the shape of the pointer.
   G8: post-P6 history is read from declared kinds; a v0 bare string is read as
   an `:activate` whose grounds are unknown, and is labeled as such rather than
   being guessed into a richer category.

   `previously-worn` is the set of revisions this master wore EARLIER in causal
   order — re-wearing one is a recovery, which is a different act from moving
   forward to a revision the master has never worn."
  [event previously-worn]
  (let [kind (:activation/kind event)
        scope-kind (first (:activation/scope event))
        rev (:activation/revision-id event)]
    (cond
      (= :pin kind) :change/pin
      (= :unpin kind) :change/unpin
      (= :rollback kind) :change/rollback
      (contains? (set previously-worn) rev) :change/recovery
      (= :scope/subject scope-kind) :change/scoped-activation
      :else :change/canonical-activation)))

(defn announcement
  "ONE activation rendered at all three scales the package promises.

   - `:announce/breath` — the local one: which appearances just changed.
   - `:announce/trace`  — the recoverable one: revision, scope, actor, and the
     REVERSAL PATH (the causally previous revision, which is what `undo this`
     actually means here — activation of a prior revision, never a delete).
   - `:announce/weather` — the ambient one: a row for the RecentChanges feed.

   `:announce/reversal` is nil at the root of the chain, and saying nil is the
   honest answer: there is nothing causally before the first activation to go
   back to."
  [{:keys [master-id facet entry previous previously-worn affected]}]
  (let [event (:event entry)
        kind (change-kind event previously-worn)]
    {:announce/master-id master-id
     :announce/facet facet
     :announce/change-kind kind
     :announce/pointer-revision-id (:pointer-revision-id entry)
     :announce/breath
     {:breath/subjects (vec (sort (or affected [])))
      :breath/revision-id (:activation/revision-id event)
      :breath/change-kind kind}
     :announce/trace
     {:trace/revision-id (:activation/revision-id event)
      :trace/scope (:activation/scope event)
      :trace/actor (:activation/actor event)
      :trace/time-ms (:activation/time-ms event)
      :trace/grounds (:activation/grounds event)
      :trace/grounds-label (activation-event/grounds-label event)
      :trace/v0? (true? (:activation/v0? event))
      :trace/reversal
      (when previous
        {:reversal/kind :rollback
         :reversal/to-revision-id
         (get-in previous [:event :activation/revision-id])
         :reversal/via-pointer-revision-id (:pointer-revision-id previous)})}
     :announce/weather
     {:weather/master-id master-id
      :weather/facet facet
      :weather/change-kind kind
      :weather/revision-id (:activation/revision-id event)
      :weather/actor-id (get-in event [:activation/actor :actor/id])
      :weather/time-ms (:activation/time-ms event)
      :weather/grounds-label (activation-event/grounds-label event)
      :weather/subject-count (count (or affected []))}}))

(defn master-announcements
  "Every activation of one master, newest first, each classified and traced.

   Walks the CAUSAL chain (T2). The `previously-worn` accumulator is built in
   causal ASCENDING order so `:change/recovery` means what it says — this
   master wore that revision before — and cannot be faked by clock order."
  [oc-rt spec {:keys [affected limit] :or {limit 20}}]
  (let [{:keys [chain regressions complete?]}
        (facet-master/activation-trail oc-rt spec)
        ascending (vec (reverse chain))
        worn-before (reductions
                     (fn [acc e] (conj acc (get-in e [:event :activation/revision-id])))
                     #{}
                     ascending)
        rows (map-indexed
              (fn [i entry]
                (announcement
                 {:master-id (facet-master/master-id spec)
                  :facet (:facet-master/facet spec)
                  :entry entry
                  ;; causally previous = the next one walking NEWEST-first
                  :previous (get chain (inc i))
                  :previously-worn (nth worn-before
                                        (- (count ascending) 1 i)
                                        #{})
                  :affected affected}))
              chain)]
    {:master-id (facet-master/master-id spec)
     :facet (:facet-master/facet spec)
     :complete? complete?
     :regressions regressions
     :announcements (vec (take limit rows))}))

;; ===========================================================================
;; Case reports — derived, claiming ONLY declared grounds
;; ===========================================================================

(defn case-report
  "Why does this master wear what it wears? Answered from the event trail and
   from nothing else.

   The discipline is in `:case/claims`: every line is generated from a DECLARED
   ground, so the report can be exhaustive without being inventive. Activations
   that declared nothing appear under `:case/unexplained` — and v0 rows, whose
   grounds were never recorded at all, appear under `:case/grounds-unknown`.
   Three buckets, because `we know it had no reason`, `we do not know its
   reason` and `here is its reason` are three different facts and collapsing
   them is exactly how a derived report starts lying."
  [oc-rt spec {:keys [limit] :or {limit 50}}]
  (let [{:keys [chain regressions complete?]}
        (facet-master/activation-trail oc-rt spec)
        entries (take limit chain)
        labeled (map (fn [e]
                       [(activation-event/grounds-label (:event e)) e])
                     entries)
        by-label (group-by first labeled)
        line (fn [e]
               (let [ev (:event e)]
                 {:claim/revision-id (:activation/revision-id ev)
                  :claim/kind (:activation/kind ev)
                  :claim/scope (:activation/scope ev)
                  :claim/actor (:activation/actor ev)
                  :claim/time-ms (:activation/time-ms ev)
                  :claim/grounds (:activation/grounds ev)}))]
    {:case/master-id (facet-master/master-id spec)
     :case/facet (:facet-master/facet spec)
     :case/complete? complete?
     :case/worn-revision-id
     (get-in (first chain) [:event :activation/revision-id])
     :case/claims (mapv (comp line second) (get by-label :grounds/declared []))
     :case/unexplained (mapv (comp line second)
                             (get by-label :grounds/ungrounded []))
     :case/grounds-unknown (mapv (comp line second)
                                 (get by-label :grounds/unknown []))
     ;; never smoothed away — a clock regression is a fact about the record
     :case/ambiguous-activation-history regressions}))

;; ===========================================================================
;; Standable history — the material world at a causal cut
;; ===========================================================================

(defn world-at
  "Project every registered master's worn revision at a causal cut.

   The cut is a map {master-id → pointer-revision-id}; masters absent from it
   stand at their current activation. Named by POINTER REVISION, never by
   timestamp — `the world at 3pm` is unanswerable when three durable
   activations honestly claim times 0, 1 and 2 (R3/T2)."
  [oc-rt cut]
  {:history/cut cut
   :history/masters
   (into (sorted-map)
         (map (fn [spec]
                (let [mid (facet-master/master-id spec)
                      at (get cut mid)]
                  [mid
                   (if at
                     (facet-master/worn-at oc-rt spec at)
                     (let [{:keys [chain]}
                           (facet-master/activation-trail oc-rt spec)
                           head (first chain)]
                       {:found? (some? head)
                        :cut :history/current
                        :worn-revision-id
                        (get-in head [:event :activation/revision-id])
                        :event (:event head)
                        :since []}))])))
         facet-masters/specs)})
