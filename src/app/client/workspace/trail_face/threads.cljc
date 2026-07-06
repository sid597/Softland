(ns app.client.workspace.trail-face.threads
  "Lanes-from-edges (trail-room R-2, CONTRACT_R2 items 5/6): fold pass ->
   connected components over lineage-kind edges -> thread identity/rank,
   band membership, move detection, and the band wrap-packer.
   PURE and ADDRESS-KEYED (CONTRACT_R2 s2.5 / PROBE-10K obligation): every
   API here returns target-id / entry-key KEYED MAPS with rank and index
   fields as DATA; no fn returns a re-sorted entry seq as a surface. All
   outputs derive from entry ATTRIBUTES (arrival-ms, ids), never from raw
   sequence position, so a permuted input yields identical output (G7a).
   rect_tree's layout engine has no wrap-flow, so band packing is new pure
   code HERE (CONTRACT_R2 s5 / v1.1 A4) - rect_tree stays untouched."
  (:require [clojure.string :as str]
            [app.client.workspace.trail-face.lanes :as lanes]))

(def lineage-kinds
  "Lane-forming kinds - the wall's lineage arrows (design R7). Connected
   components run over these ONLY. CONTRACT_R2 trap 2: cross-link kinds
   (:references :elaborates) NEVER merge lanes - a references chain must
   not thread unrelated work into mush; they render as kraft connectors
   BETWEEN lanes (G4)."
  #{:based-on :produced :built-over :new-direction})

(defn- target-id [e] (get-in e [:entry/target :id]))

(defn- ekey
  "Entry identity [target-id arrival-ms] - the same computation as
   cards/entry-key (kept local so this ns stays dependency-light; the
   lanes ns sets the same precedent)."
  [e]
  [(target-id e) (:time/arrival-ms e)])

(defn- rt-entry? [e] (= :relation-transition (:entry/kind e)))

(defn du-id?
  "A derived-unit (block/message) id - a fold CHILD, never a fold head
   and never a thread-identity member (CONTRACT_R2 s2.3: 'd' < 'o' would
   pin a thread's identity to an invisible row)."
  [id]
  (str/starts-with? (str id) "du:"))

;; ── Fold pass (s1 item 5: family-key demotes from lane-maker to FOLD rule;
;;    fold FIRST, then the folded unit is assigned by the fold HEAD's edges) ──

(defn fold-index
  "entries -> {:key-of   {target-id fold-key}
               :head-of  {fold-key head-target-id}
               :carrier  {fold-key carrier-entry-key}   (terrain families only)
               :members  {fold-key [entry-key ...]}      (arrival-asc, keyed)
               :count    {fold-key n-folded-beyond-carrier}}
   The fold groups a doc with its du: blocks and a conversation with its
   messages (lanes/entry-thread-key - the demoted family rule). The HEAD is
   the DOC: the lexicographically-smallest non-du member id (du ids are fold
   children - s2.3/S3); all-du families fall back to the smallest du id
   honestly. The CARRIER is the terrain entry that paints the fold's ONE
   card (G5): the family's newest-arrival terrain entry (attribute-derived,
   deterministic; tiebreak target-id)."
  [entries]
  (let [groups (group-by lanes/entry-thread-key entries)]
    (reduce
     (fn [acc [k es]]
       (let [tids    (distinct (map target-id es))
             head    (first (sort-by (fn [id] [(if (du-id? id) 1 0) (str id)]) tids))
             terrain (remove rt-entry? es)
             carrier (when (seq terrain)
                       (ekey (last (sort-by (fn [e] [(:time/arrival-ms e)
                                                     (str (target-id e))])
                                            terrain))))
             members (mapv ekey (sort-by (fn [e] [(:time/arrival-ms e)
                                                  (str (target-id e))])
                                         es))]
         (-> acc
             (update :key-of into (map (fn [tid] [tid k])) tids)
             (assoc-in [:head-of k] head)
             (cond-> carrier (assoc-in [:carrier k] carrier))
             (assoc-in [:members k] members)
             (cond-> carrier (assoc-in [:count k] (dec (count es)))))))
     {:key-of {} :head-of {} :carrier {} :members {} :count {}}
     ;; sort groups for deterministic accumulation (maps compare by value
     ;; anyway, but keep the walk order canonical too)
     (sort-by (comp str key) groups))))

(defn lift-id
  "target-id -> its fold head (s1 item 5 v1.1/S3: a folded member's own
   lineage edge counts as the HEAD's for component purposes). Ids outside
   any entry family (bare edge far-ends) lift through family-key when the
   family has entries, else stay themselves."
  [fold id]
  (or (when-let [k (get (:key-of fold) id)] (get (:head-of fold) k))
      (get (:head-of fold) (lanes/family-key id))
      id))

;; ── Lineage edges + connected components (s2.1/s2.3) ─────────────────────────

(defn- rt-lineage-row [e]
  (let [d (:entry/detail e)]
    (when (and (rt-entry? e)
               (contains? lineage-kinds (:kind d))
               (get-in d [:from :id]) (get-in d [:to :id]))
      {:kind (:kind d)
       :status (or (:status d) :asserted)
       :relation-id (:relation-id d)
       :from-id (get-in d [:from :id])
       :to-id (get-in d [:to :id])
       :arrival (:time/arrival-ms e)})))

(defn current-lineage-rows
  "entries -> the CURRENT asserted lineage assertions, UNLIFTED endpoints:
   per relation-id the latest transition (max arrival) wins, and a
   retracted final state DROPS the edge - threading material on a
   withdrawn assertion would be the map lying. Sorted vector (canonical)."
  [entries]
  (let [rows   (keep rt-lineage-row entries)
        ;; total order per relation-id (gate fix, R2 falsification S2): strict
        ;; arrival-> alone leaves same-ms transitions to INPUT ORDER, breaking
        ;; s2.5 canonicalization. Tiebreak by status/kind strings — total, and
        ;; :retracted > :asserted lexically, so a same-ms assert+retract pair
        ;; deterministically resolves RETRACTED (when in doubt, don't thread).
        newer? (fn [r prev]
                 (pos? (compare [(:arrival r) (str (:status r)) (str (:kind r))]
                                [(:arrival prev) (str (:status prev)) (str (:kind prev))])))
        latest (reduce (fn [m r]
                         (let [k (or (:relation-id r)
                                     [(:from-id r) (:to-id r) (:kind r)])
                               prev (get m k)]
                           (if (or (nil? prev) (newer? r prev))
                             (assoc m k r)
                             m)))
                       {} rows)
        kept   (filter #(= :asserted (:status %)) (vals latest))]
    (vec (sort-by (juxt :from-id :to-id (comp str :kind)) kept))))

(defn lineage-edges
  "entries + fold -> sorted vector of fold-LIFTED {:kind :from-id :to-id}
   component edges (lineage kinds only - trap 2 pins the set at
   `lineage-kinds` above)."
  [entries fold]
  (vec (sort-by (juxt :from-id :to-id (comp str :kind))
                (map (fn [r] {:kind (:kind r)
                              :from-id (lift-id fold (:from-id r))
                              :to-id (lift-id fold (:to-id r))})
                     (current-lineage-rows entries)))))

(defn components
  "lifted edges -> {node-id root-id}. Union-find with union-BY-MIN: the
   root is always the lexicographically smallest member id, which IS the
   thread identity (s2.3): deterministic, stable under growth, and a merge
   keeps the smaller id so chips fire only for the changed side (validator
   trace B). Result is independent of edge order."
  [edges]
  (letfn [(find-root [parent x]
            (let [p (get parent x x)]
              (if (= p x) x (recur parent p))))]
    (let [nodes  (distinct (mapcat (juxt :from-id :to-id) edges))
          parent (reduce (fn [parent {:keys [from-id to-id]}]
                           (let [ra (find-root parent from-id)
                                 rb (find-root parent to-id)]
                             (cond
                               (= ra rb) parent
                               (neg? (compare ra rb)) (assoc parent rb ra)
                               :else (assoc parent ra rb))))
                         {} edges)]
      ;; EVERY node maps to its root - including the roots themselves
      ;; (a root never appears as a parent-map KEY; dropping it would
      ;; silently band a thread's own identity member)
      (into {} (map (fn [k] [k (find-root parent k)])) nodes))))

;; ── Assignment (the ONE keyed transform the scene consumes) ──────────────────

(defn assign
  "entries -> {:assignment   {target-id thread-id-or-:band}
               :thread-rank  {thread-id rank}         (rank as DATA - Delta-9)
               :thread-index {entry-key idx}          (in-thread, arrival-asc)
               :fold         (fold-index entries)
               :edges        lifted lineage edges}
   Threads exist ONLY where lineage edges do; every other target is :band -
   the self-declaring frontier, never auto-filed into fake lanes (design R7).
   Rank = order of first appearance of each thread's EARLIEST member by
   arrival-ms (design R7 ordering DEFAULT), tiebreak thread-id. Everything
   derives from attributes, never sequence position (s2.5)."
  [entries]
  (let [fold  (fold-index entries)
        edges (lineage-edges entries fold)
        comps (components edges)
        tids  (distinct (map target-id entries))
        assignment (into {}
                         (map (fn [tid] [tid (get comps (lift-id fold tid) :band)]))
                         tids)
        firsts (reduce (fn [m e]
                         (let [th (get assignment (target-id e) :band)]
                           (if (= :band th)
                             m
                             (update m th (fn [cur]
                                            (let [a (:time/arrival-ms e)]
                                              (if cur (min cur a) a)))))))
                       {} entries)
        ranks  (into {}
                     (map-indexed (fn [i [th _]] [th i]))
                     (sort-by (fn [[th f]] [f (str th)]) firsts))
        tindex (reduce
                (fn [m [_ es]]
                  (into m (map-indexed (fn [i e] [(ekey e) i])
                                       (sort-by (fn [e] [(:time/arrival-ms e)
                                                         (str (target-id e))])
                                                es))))
                {}
                (sort-by (comp str key)
                         (group-by (fn [e] (get assignment (target-id e) :band))
                                   entries)))]
    {:assignment assignment
     :thread-rank ranks
     :thread-index tindex
     :fold fold
     :edges edges}))

;; ── Move detection (item 6 / s2.4: PURE - state lives with the caller) ───────

(defn display-names
  "entries -> {target-id display-name} for targets that carry one (G6/S6:
   the far end's display-name WHEN PRESENT, else its raw id - honest)."
  [entries]
  (reduce (fn [m e]
            (let [t (:entry/target e)]
              (if (:display-name t) (assoc m (:id t) (:display-name t)) m)))
          {} entries))

(defn moves
  "prev-assign + prev-edges + new-assign + new-edges + entries ->
   {target-id {:from .. :to .. :reason ..}}.
   A target moves ONLY when (a) it is present in BOTH maps with a CHANGED
   value AND (b) its fold head is an endpoint of a CHANGED edge (symmetric
   difference of the lifted edge sets) - gate fix (R2 falsification S1):
   a thread MERGE re-roots the absorbed side, changing every member's
   assignment VALUE, but only the merging edge's endpoints actually did
   anything; firing 'joined thread' on bystanders violates G6's
   'non-movers carry none' (the validator-trace intent: chips fire only
   for the changed side). A brand-new target is an arrival (the rim's
   delta slot), never a move; an empty/nil prev (first pull) moves
   nothing. Fold-head drift across pulls makes a family's edges read as
   changed - honest (the family visibly changed shape). The reason is
   sayable (design R7): 'joined thread · <kind> <far-end
   display-name-or-id>', the causing edge picked deterministically from
   the ADDED incident edges first (smallest [kind far-id]), falling back
   to any current incident edge; a fall to :band says so honestly. Same
   inputs -> identical output (G6 determinism)."
  [prev-assign prev-edges new-assign new-edges entries]
  (if (empty? prev-assign)
    {}
    (let [names (display-names entries)
          fold  (fold-index entries)
          rows  (current-lineage-rows entries)
          added   (vec (remove (set (or prev-edges [])) new-edges))
          removed (vec (remove (set new-edges) (or prev-edges [])))
          changed-heads (into #{} (mapcat (juxt :from-id :to-id))
                              (concat added removed))
          incident-in (fn [h edges]
                        (sort-by (fn [x] [(str (:kind x)) (str (:far-id x))])
                                 (keep (fn [ed]
                                         (cond
                                           (= h (:from-id ed))
                                           {:kind (:kind ed) :far-id (:to-id ed)}
                                           (= h (:to-id ed))
                                           {:kind (:kind ed) :far-id (:from-id ed)}
                                           :else nil))
                                       edges)))
          incident (fn [tid]
                     (let [h (lift-id fold tid)]
                       (or (seq (incident-in h added))
                           (seq (incident-in h new-edges)))))
          reason (fn [tid to]
                   (if (= :band to)
                     "left thread · no asserted lineage"
                     (if-let [{:keys [kind far-id]} (first (incident tid))]
                       (str "joined thread · " (name kind) " "
                            (or (get names far-id) far-id))
                       (str "joined thread · " to))))]
      (into {}
            (keep (fn [[tid to]]
                    (when-let [from (get prev-assign tid)]
                      (when (and (not= from to)
                                 (contains? changed-heads (lift-id fold tid)))
                        [tid {:from from :to to :reason (reason tid to)}]))))
            new-assign))))

;; ── Band wrap-packer (s1 item 5 / v1.1 A4) ───────────────────────────────────

(defn pack-band
  "Wrap-packing INSIDE the band region ONLY (trap 4: mod-wrap NEVER applies
   across threaded lanes - the F-L2 staircase must not return by the other
   door; rect_tree has no wrap-flow, so this pure packer owns it).
   cells = [{:key k :w w :h h}] already in READING ORDER (= time order, G3);
   band-w = available width; gap = both axes. Returns {key {:x :y :row}} -
   a KEYED map, never a reordered seq (s2.5). Rows advance by the tallest
   cell in the row; a cell wider than band-w gets its own row."
  [cells band-w gap]
  (loop [cs cells, x 0, y 0, row 0, row-h 0, out {}]
    (if (empty? cs)
      out
      (let [{:keys [key w h]} (first cs)
            wrap? (and (pos? x) (> (+ x w) band-w))
            x'    (if wrap? 0 x)
            y'    (if wrap? (+ y row-h gap) y)
            row'  (if wrap? (inc row) row)
            rh    (if wrap? h (max row-h h))]
        (recur (rest cs) (+ x' w gap) y' row' rh
               (assoc out key {:x x' :y y' :row row'}))))))
