(ns app.client.workspace.trail-face.lanes
  "Thread/lane assignment + Manhattan connector geometry (pure cljc).
   Lanes group feed entries into threads by conversation / target
   family; connectors are AXIS-ALIGNED THIN RECTS ONLY (INV-17, trap 6:
   no line/bezier pipeline - curves are a Sid-gated follow-up).
   Assignment is DETERMINISTIC: same input -> identical output (gate 7)."
  (:require [clojure.string :as str]))

(def connector-thickness 2)
(def junction-size 4)

;; --- Family / thread keys -----------------------------------------------------

(defn- strip-trailing-ordinal
  "du:<key>:000003 -> du:<key> class grouping: drop trailing all-digit
   segments so members join their family."
  [segments]
  (if (and (> (count segments) 1)
           (re-matches #"[0-9]+" (peek segments)))
    (recur (pop segments))
    segments))

(defn family-key
  "Deterministic thread key for a target id. Groups a doc with its
   blocks/units and a conversation with its messages:
   oc:<kind>:<rest> -> <rest minus trailing ordinals>
   du:<key>:<n>     -> <key>
   anything else    -> the id itself."
  [id]
  (let [s (str id)
        segs (str/split s #":")]
    (cond
      (and (= "oc" (first segs)) (> (count segs) 2))
      (str/join ":" (strip-trailing-ordinal (vec (drop 2 segs))))

      (and (= "du" (first segs)) (> (count segs) 1))
      (second segs)

      :else s)))

(defn entry-thread-key
  "Conversation from :entry/detail wins; else the target's family."
  [entry]
  (or (get-in entry [:entry/detail :conversation])
      (family-key (get-in entry [:entry/target :id]))))

(defn assign-lanes
  "Entries -> {entry-key lane-index}. Lane index = order of FIRST
   appearance of the thread key in the (already-ordered) entry seq -
   deterministic, no hashing, no randomness (gate 7)."
  [entries]
  (let [entry-key (fn [e] [(get-in e [:entry/target :id]) (:time/arrival-ms e)])]
    (loop [es entries, lane-of {}, next-lane 0, out {}]
      (if (empty? es)
        out
        (let [e  (first es)
              tk (entry-thread-key e)
              [lane-of' next-lane'] (if (contains? lane-of tk)
                                      [lane-of next-lane]
                                      [(assoc lane-of tk next-lane) (inc next-lane)])]
          (recur (rest es) lane-of' next-lane'
                 (assoc out (entry-key e) (get lane-of' tk))))))))

;; --- Dead ends (face law 8, D-002) ---------------------------------------------

(defn dead-end-ids
  "Target ids marked dead-end by a :dead-end relation edge."
  [edges]
  (into #{}
        (comp (filter #(= :dead-end (:kind %)))
              (keep #(get-in % [:from :id])))
        edges))

(defn dead-end-terminal?
  "Is this target terminal? Accepts the edge-derived set + the entry's
   own detail flag (fixture-friendly, total)."
  [target-id dead-ends entry]
  (boolean (or (contains? dead-ends target-id)
               (get-in entry [:entry/detail :dead-end?])
               (= :dead-end (get-in entry [:entry/detail :kind])))))

;; --- Manhattan connectors (thin rects only) -------------------------------------

(defn- hrect [x1 x2 y kind status]
  (let [xa (min x1 x2) xb (max x1 x2)]
    {:x xa :y (- y (/ connector-thickness 2))
     :w (max connector-thickness (- xb xa)) :h connector-thickness
     :connector {:kind kind :status status :segment :elbow}}))

(defn- vrect [x y1 y2 kind status]
  (let [ya (min y1 y2) yb (max y1 y2)]
    {:x (- x (/ connector-thickness 2)) :y ya
     :w connector-thickness :h (max connector-thickness (- yb ya))
     :connector {:kind kind :status status :segment :spine}}))

(defn- junction [x y kind status]
  {:x (- x (/ junction-size 2)) :y (- y (/ junction-size 2))
   :w junction-size :h junction-size
   :connector {:kind kind :status status :segment :junction}})

(defn connector-rects
  "One relation edge between two card bounds -> thin rects:
   vertical spine out of the FROM card + horizontal elbow into the TO
   card + a junction dot at the turn. Both endpoints TOUCH their card's
   bounds (gate 7). :retracted renders struck/dim but still visible."
  [{:keys [kind status]} from-b to-b]
  (let [status  (or status :asserted)
        from-cx (+ (:x from-b) (/ (:w from-b) 2))
        to-cy   (+ (:y to-b) (/ (:h to-b) 2))
        going-down? (>= to-cy (+ (:y from-b) (:h from-b)))
        spine-y1 (if going-down? (+ (:y from-b) (:h from-b)) (:y from-b))
        to-edge-x (if (<= from-cx (:x to-b)) (:x to-b) (+ (:x to-b) (:w to-b)))]
    [(vrect from-cx spine-y1 to-cy kind status)
     (hrect from-cx to-edge-x to-cy kind status)
     (junction from-cx to-cy kind status)]))

(defn connectors
  "Edges + card-bounds ({target-id {:x :y :w :h}}) -> flat vector of
   thin rects. Edges whose endpoints have no cards on screen emit
   nothing (they are bundle data, not floating geometry)."
  [edges card-bounds]
  (into []
        (mapcat (fn [e]
                  (let [fb (get card-bounds (get-in e [:from :id]))
                        tb (get card-bounds (get-in e [:to :id]))]
                    (when (and fb tb)
                      (connector-rects e fb tb)))))
        edges))

(defn lane-spines
  "Per-lane vertical thread spine connecting consecutive cards
   (top->bottom). A dead-end-marked card TERMINATES its lane: NO
   segment is emitted past it (face law 8 - ghost-lane falsifier).
   cards = [{:entry-key k :target-id id :bounds {..} :dead-end? b}],
   already in visual (y) order; lanes = {entry-key lane}."
  [cards lanes]
  (let [by-lane (group-by (fn [c] (get lanes (:entry-key c))) cards)]
    (into []
          (mapcat
           (fn [[_lane lane-cards]]
             (let [ordered (sort-by #(get-in % [:bounds :y]) lane-cards)]
               (loop [cs ordered, segs []]
                 (let [[a b] [(first cs) (second cs)]]
                   (cond
                     (nil? b) segs
                     ;; terminal card: stop the spine here - nothing forward
                     (:dead-end? a) segs
                     :else
                     (let [ax (+ (get-in a [:bounds :x])
                                 (/ (get-in a [:bounds :w]) 2))
                           y1 (+ (get-in a [:bounds :y]) (get-in a [:bounds :h]))
                           y2 (get-in b [:bounds :y])]
                       (recur (rest cs)
                              (conj segs (vrect ax y1 y2 :lane :asserted))))))))))
          by-lane)))
