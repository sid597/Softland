(ns app.server.worn.material-truth
  "Instance discovery and read projections over revisioned facet masters.
   Given a caller-owned object-container runtime and specs/subjects, returns
   served instance maps, scope estimates, activation announcements, declared
   grounds and per-master history cuts. Write wrappers call facet-master first,
   then best-effort episode registry upkeep; they return the facet write result.

   Owns no mutable state or resources. Object-container holds material truth;
   episode holds the durable discovery registry. Serving performs individual
   foreign reads, not a batched or atomic snapshot. Pure facet-engine resolves
   the resulting values; this namespace neither renders nor dispatches verbs."
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
  "After an accepted facet write, attempt to register its subject/instance pair in
   the episode discovery index. Return the original write result even when index
   upkeep throws; the caught :index-failed map is discarded. Discovery may lag
   durable material. No index failure is surfaced in this function's result."
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
  "Write deviation material, then best-effort index it in the requested conversation."
  [oc-rt parent-spec subject-uid overrides opts]
  (register! oc-rt parent-spec subject-uid
             (facet-master/deviate! oc-rt parent-spec subject-uid
                                    overrides opts)
             opts))

(defn release-deviation!
  "Write inherited-state rollback, then best-effort retain its discovery entry."
  [oc-rt parent-spec subject-uid opts]
  (register! oc-rt parent-spec subject-uid
             (facet-master/release-deviation! oc-rt parent-spec subject-uid opts)
             opts))

(defn pin!
  "Write the subject pin through facet-master, then best-effort index the instance."
  [oc-rt parent-spec subject-uid pinned-revision-id opts]
  (register! oc-rt parent-spec subject-uid
             (facet-master/pin! oc-rt parent-spec subject-uid
                                pinned-revision-id opts)
             opts))

(defn unpin!
  "Clear the subject pin through facet-master, then best-effort index the instance."
  [oc-rt parent-spec subject-uid opts]
  (register! oc-rt parent-spec subject-uid
             (facet-master/unpin! oc-rt parent-spec subject-uid opts)
             opts))

(defn rebuild-instance-registry!
  "Probe every registered shared spec against supplied candidate subjects and
   register pairs whose instance document has a latest revision. Returns the
   registry write result plus :rebuilt entries. Does not scan all stored masters
   or remove old registry rows; caller must supply the relevant subject universe."
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
;; The served instance tier — registry discovery followed by per-instance reads
;; ===========================================================================

(defn- compiled-pin-target
  "Read a pinned revision and compile its content under the parent spec.
   A missing target yields explicit invalid data; nil id yields nil. Wear-time
   resolution can then use the supplied result without doing I/O. Foreign read
   failures propagate and this reader does not check the revision container id."
  [oc-rt parent-spec pinned-revision-id]
  (when pinned-revision-id
    (let [rev (ocr/read-revision oc-rt pinned-revision-id)]
      (if (nil? rev)
        {:valid? false :revision-id pinned-revision-id
         :errors [{:type :facet-master/pinned-revision-missing}]}
        (assoc (facet-engine/compile-source parent-spec (:content-text rev))
               :revision-id pinned-revision-id)))))

(defn served-instance
  "Read one subject's instance state and optional pinned target into the map
   consumed by wear-for-subject. Return nil without a latest instance revision.
   This still performs foreign reads when absent; it is not a free lookup."
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
  "Return {facet -> {subject -> served-instance}} for discovered or extra subjects.
   Read the episode registry once, union its pairs with each registered facet
   crossed with extra-subjects, then read instances individually in stable order.
   Registry read failures are treated as an empty index; extra-subjects can still
   discover instances. No batched foreign query or atomic snapshot is used."
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
  "Estimate a declared scope over caller-supplied candidate-wearers without writing.
   Read instance projections and classify valid pins/deviations separately from
   inherited or invalid instances. Subject scope includes only that subject if
   it occurs in the supplied set; all-unpinned also marks future wearers covered.

   This is a classification of current instance state, not a simulation of a
   candidate revision or a historical affected set. It neither discovers every
   wearer nor validates the proposed scope. The default case treats any other
   scope kind as all-unpinned."
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
  "Project a supplied activation entry as local subjects (:announce/breath),
   detailed event/reversal data (:announce/trace) and a feed row (:announce/weather).
   previous supplies the reversal target; affected supplies the subject set.
   Neither is discovered here. :trace/reversal is nil when previous is absent."
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
  "Classify the fetched causal history and return at most limit rows, newest first.
   Recovery means rewearing a revision already seen earlier in that fetched chain.
   The same caller-supplied affected set is attached to every row; this is not a
   reconstruction of past wearer sets. :complete? is inherited from the bounded
   activation-trail read and does not account for this function's output limit."
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
  "Group up to limit entries from the fetched causal chain by declared, empty or
   unknown grounds. Claims repeat recorded grounds without verifying them or
   inferring reasons. Empty grounds means none were recorded, not that no reason
   existed. Clock regressions and activation-trail's limited completeness flag
   are returned; the flag does not account for this function's output limit."
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
  "Read pointer cuts for the static shared-master registry. cut maps master ids
   to pointer revision ids; omitted masters use their fetched current trail head.
   Each read has activation-trail's page limit. This is not an atomic global
   snapshot, instance-history reconstruction or rendering of the historical world."
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
