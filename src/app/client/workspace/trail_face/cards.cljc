(ns app.client.workspace.trail-face.cards
  "Kernel-material card builders (pure cljc). A SEPARATE builder set
   from any agent-run card surface (trap 10): these render WP1 shapes
   (feed entries, bundle layers, verdicts, omissions, holes).
   Every fn is (data, geometry) -> data; `now-ms` where staleness needs
   it is an ARGUMENT (no wall clock in pure code); char-advance is an
   ARGUMENT (S3 - no 0.56 literal in this directory).
   Display glyphs are raw printable UTF-8 (S4 bans control bytes only);
   every glyph still passes sanitize-tree, so an uncovered one degrades
   to U+FFFD honestly."
  (:require [clojure.string :as str]
            [app.client.workspace.rect-tree :as rt]))

;; --- Pure UTC date math (no java.time / js Date interop; S2) ---------------

(defn ms->utc-ymd
  "Epoch ms -> [year month day] in UTC. Howard Hinnant's civil_from_days,
   pure integer arithmetic; valid for all post-1970 stamps."
  [ms]
  (let [days (quot ms 86400000)
        z    (+ days 719468)
        era  (quot z 146097)
        doe  (- z (* era 146097))
        yoe  (quot (- doe
                      (quot doe 1460)
                      (- (quot doe 36524))
                      (quot doe 146096))
                   365)
        y    (+ yoe (* era 400))
        doy  (- doe (+ (* 365 yoe) (quot yoe 4) (- (quot yoe 100))))
        mp   (quot (+ (* 5 doy) 2) 153)
        d    (+ (- doy (quot (+ (* 153 mp) 2) 5)) 1)
        m    (+ mp (if (< mp 10) 3 -9))
        y    (if (<= m 2) (inc y) y)]
    [y m d]))

(defn- pad2 [n] (if (< n 10) (str "0" n) (str n)))

(defn ms->date-str
  "Epoch ms -> \"2026-04-28\" (UTC). nil-safe: nil -> \"unknown\"."
  [ms]
  (if (nil? ms)
    "unknown"
    (let [[y m d] (ms->utc-ymd ms)]
      (str y "-" (pad2 m) "-" (pad2 d)))))

(def middot "·")

;; --- Addressable strings (item 2 / R5) --------------------------------------

(defn address->addressable-string
  "An address EDN form -> a compact addressable STRING for chrome/expansion
   display (item 2 / R5: the addressable string itself, NEVER a `pr-str` map
   dump). Total over the WP1 address shapes (->address = `(trail/<verb>
   <params>)`, trail_view.clj):
     (trail/context-bundle {:targets [id ...]}) -> the primary element id
     (trail/recent-activity {... :order o})     -> \"recent-activity · <order>\"
     (trail/<verb> {...})                        -> \"<verb>\"
     string / nil / other                        -> itself / \"—\" / (pr-str x)"
  [address]
  (cond
    (and (seq? address) (seq address) (symbol? (first address)))
    (let [verb   (name (first address))
          params (when (map? (second address)) (second address))]
      (cond
        (and (= verb "context-bundle") (seq (:targets params)))
        (str (first (:targets params)))
        (and (= verb "recent-activity") (:order params))
        (str verb " " middot " " (name (:order params)))
        :else verb))
    (string? address) address
    (nil? address)    "—"
    :else             (pr-str address)))

;; --- Two clocks (face law 4; WP1 s6) ----------------------------------------

(defn two-clock-stamp
  "Entry -> stamp string. Renders BOTH stamps when they diverge
   (different UTC dates); a nil claimed-ms is honest (`claimed unknown`),
   never invented. Single stamp only when the two clocks agree."
  [{claimed :time/claimed-ms arrival :time/arrival-ms}]
  (let [cd (when claimed (ms->date-str claimed))
        ad (ms->date-str arrival)]
    (cond
      (nil? claimed) (str "arrived " ad " " middot " claimed unknown")
      (= cd ad)      ad
      :else          (str "claimed " cd " " middot " arrived " ad))))

(defn- ms->compact-date-str
  "MM-DD when the stamp shares `ref-ms`'s UTC year, else the full date -
   compression never hides a cross-year divergence (the map must not lie)."
  [ms ref-ms]
  (let [[y m d] (ms->utc-ymd ms)
        [ry _ _] (ms->utc-ymd ref-ms)]
    (if (= y ry)
      (str (pad2 m) "-" (pad2 d))
      (str y "-" (pad2 m) "-" (pad2 d)))))

(defn compressed-two-clock-stamp
  "R-2 band-2 reading line stamp (CONTRACT_R2 s1 item 1 / design R5:
   'claimed 07-04 · arrived 07-05'). Same honesty rules as two-clock-stamp -
   nil claimed renders `claimed unknown` (the t4-spine claimed-ms field is
   consumed AS DATA; honest nil until it lands), equal UTC dates compress
   to one stamp, years print only on cross-year divergence."
  [{claimed :time/claimed-ms arrival :time/arrival-ms}]
  (cond
    (nil? claimed)
    (str "arrived " (ms->compact-date-str arrival arrival)
         " " middot " claimed unknown")

    (= (ms->date-str claimed) (ms->date-str arrival))
    (ms->compact-date-str arrival arrival)

    :else
    (str "claimed " (ms->compact-date-str claimed arrival)
         " " middot " arrived " (ms->compact-date-str arrival arrival))))

;; --- Staleness triad (face law 5; WP1 L5) -----------------------------------

(def stale-after-ms
  "Attested longer ago than this reads as stale (7 days)."
  (* 7 86400000))

(def staleness-styles
  "Three PAIRWISE-DISTINCT treatments (gate 9). nil must not read fresh.
   Markers: U+25CF filled / U+25D0 half / U+25CB empty circle."
  {:attested-recent {:rgba [0.55 0.85 0.65 1.0] :marker "●"}
   :attested-stale  {:rgba [0.85 0.75 0.45 1.0] :marker "◐"}
   :never-attested  {:rgba [0.60 0.60 0.70 1.0] :marker "○"}})

(defn staleness-treatment
  "WP1 L5 :times -> {:state kw :label str :rgba [..] :marker str}.
   `last-attested-ms` nil => :never-attested (`walked unknown` - absence
   of attestation must NOT read as freshness). now-ms is an argument."
  [{:keys [last-attested-ms]} now-ms]
  (let [state (cond
                (nil? last-attested-ms)                        :never-attested
                (< (- now-ms last-attested-ms) stale-after-ms) :attested-recent
                :else                                          :attested-stale)
        label (case state
                :never-attested (str "attested never " middot " walked unknown")
                :attested-recent (str "attested " (ms->date-str last-attested-ms))
                :attested-stale  (str "attested " (ms->date-str last-attested-ms)
                                      " (stale)"))]
    (merge {:state state :label label} (staleness-styles state))))

;; --- Provenance badges (face law 6; WP1 s5.1) --------------------------------

(defn provenance-badges
  "Row (edge / verdict / entry-actor) -> badge vector.
   :asserted-by ALWAYS present; :written-by badge ONLY when it differs
   from the asserter (WP1 s5.1 projection rule)."
  [{:keys [asserted-by written-by]}]
  (cond-> [{:badge :asserted-by :text (str asserted-by)}]
    (and written-by (not= written-by asserted-by))
    (conj {:badge :written-by :text (str "written-by " written-by)})))

;; --- Verdict fold (face law 6; WP1 L4) ---------------------------------------

(defn- disagreeing-asserters
  "Disagreement = two asserters whose current verdicts on the SAME judged
   target (:to endpoint - WP1 stance rows are full edge rows) differ in
   kind."
  [current]
  (let [by-object (group-by (fn [[_ v]] (get-in v [:to :id]))
                            (for [[asserter vs] current, v vs]
                              [asserter v]))]
    (into #{}
          (comp (filter (fn [[_ avs]]
                          (> (count (into #{} (map (comp :kind second)) avs)) 1)))
                (mapcat (fn [[_ avs]] (map first avs))))
          by-object)))

(defn verdict-lines
  "L4 :verdicts.:current -> [{:text .. :badges .. :disagrees? ..}] - one
   line per (asserter, verdict). Verdict entries are FULL stance edge
   rows (live WP1 shape, gate-16 verified): the judged target is :to,
   the via/evidence context is :from, the stamp is :last-changed-at-ms.
   Disagreeing asserters BOTH render, badged, NEVER merged."
  [current]
  (let [disagree (disagreeing-asserters current)]
    (vec
     (for [[asserter vs] (sort-by key current)
           v vs]
       (let [dis? (contains? disagree asserter)]
         {:text (str asserter ": " (name (:kind v)) " " (get-in v [:to :id])
                     (when-let [via (get-in v [:from :id])] (str " via " via))
                     " @" (ms->date-str (or (:last-changed-at-ms v)
                                            (:first-asserted-at-ms v)))
                     (if dis?
                       (str " (current — disagrees)")
                       " (current)"))
          :badges (provenance-badges {:asserted-by (or (:asserted-by v) asserter)
                                      :written-by (:written-by v)})
          :disagrees? dis?
          :style :verdict})))))

;; --- Omissions (face law 7 - omissions are pixels) ---------------------------

(defn omission-line
  "One omission entry -> one VISIBLE line with its count; resumable caps
   render the cursor affordance (E-15). Total over the three live shapes
   (gate-16 verified): bundle caps {:layer :dropped :cap :cursor},
   unrecognized targets {:id :reason <kw>}, and feed standing gaps
   {:omission/kind :feed/uncovered :feed/gap <kw> :reason <string>}."
  [{:keys [layer dropped cap cursor reason note] :as omission}]
  (let [uncovered? (or (= :feed/uncovered (:omission/kind omission))
                       (= :feed/uncovered reason))]
    {:style :omission
     :omission omission
     :text (cond
             uncovered?
             (str "uncovered"
                  (when-let [g (:feed/gap omission)] (str " (" (name g) ")"))
                  ": " (or note (when (string? reason) reason) "declared gap"))

             (keyword? reason)
             ;; full keyword incl. namespace - :target/unrecognized carries
             ;; meaning in the namespace half
             (str "omitted (" (subs (str reason) 1) "): "
                  (or (:id omission) "") (when dropped (str " x" dropped)))

             :else
             (str "omitted " (or dropped "?") " of " (some-> layer name)
                  (when cap (str " (cap " cap ")"))
                  (when cursor " … more")))}))

(defn omissions-block
  "Omission entries -> rt-node block, one visible line each. An empty
   input renders NO block (nil), never an empty frame."
  [omissions {:keys [line-height card-w] :as _geom}]
  (when (seq omissions)
    (let [lines (mapv omission-line omissions)]
      (rt/rt-node [:trail-face/omissions (count lines)] :omissions
                  {:x 0 :y 0 :w (or card-w 300)
                   :h (* (count lines) (or line-height 18))}
                  :style {:bg [0.25 0.20 0.20 0.6]}
                  :data {:trail-face/omission-count (count lines)}
                  :text (vec (map-indexed
                              (fn [i l]
                                {:text (:text l) :x 4 :y (* i (or line-height 18))
                                 :size 12 :style :omission})
                              lines))))))

;; --- Hole endpoint (gate 12 - question-unit door stays open) -----------------

(defn hole-endpoint?
  "An endpoint that names nothing resolvable: kind :hole/:unresolved or
   a missing id. Total - any shape, even nil, answers."
  [endpoint]
  (or (nil? endpoint)
      (nil? (:id endpoint))
      (contains? #{:hole :unresolved} (:kind endpoint))))

(defn hole-endpoint-card
  "A hole-shaped row renders with the EXPLICIT hole style (U+25CC dotted
   circle) - no exception, no silent skip (WP1 s9.3 door kept open)."
  [row {:keys [card-w line-height] :as _geom}]
  (rt/rt-node [:trail-face/hole (or (:relation-id row) (hash row))] :hole
              {:x 0 :y 0 :w (or card-w 300) :h (or line-height 18)}
              :style {:bg [0.30 0.25 0.35 0.6] :border-width 1
                      :border-color [0.6 0.5 0.8 0.8]}
              :data {:trail-face/hole? true}
              :text [{:text (str "◌ hole "
                                 (when-let [k (:kind row)] (str "(" (name k) ") "))
                                 "- endpoint unresolved")
                      :x 4 :y 0 :size 12 :style :hole}]))

;; --- Material preview (expansion; WP1 s9.6 no-paraphrase) --------------------

(defn material-preview
  "MECHANICAL truncation only - the face never summarizes. Truncation is
   an omission and says so visibly."
  [{:keys [content-text] :as _material} max-chars]
  (let [t (or content-text "")]
    (if (> (count t) max-chars)
      {:text (subs t 0 max-chars) :truncated? true
       :notice (str "… " (- (count t) max-chars) " chars truncated")}
      {:text t :truncated? false})))

;; --- Feed-entry card (OP-9) ---------------------------------------------------

(def kind-glyph
  "U+2194 both-arrow / U+270E pencil / U+2193 down arrow.
   All three are in the regenerated atlas (U+2913 is in neither font)."
  {:relation-transition     "↔"
   :transcript-file-updated "✎"
   :source-ingested         "↓"})

(defn entry-key
  "Deterministic identity for a feed entry (fixtures carry no entry id)."
  [entry]
  [(get-in entry [:entry/target :id]) (:time/arrival-ms entry)])

(def move-chip-glyph
  "U+2605 BLACK STAR - atlas-verified 2026-07-06 (trap 10). The new-since
   chip marker (item 6: every assignment change announces itself)."
  "★")

(def fold-chip-glyph
  "ASCII '+' - zero glyph risk. Band-0 fold chip prefix (G5: at band 0 the
   fold renders as a chip with a count). U+2295 (circled plus) is ABSENT
   from the merged atlas (verified 2026-07-06), so plain + carries it."
  "+")

(defn- band-line-ops
  "Entry + band -> the LINE ops (typography - CONTRACT_R2 s1 item 1 / R4:
   bands 0-2 emit line ops, no box, no fill):
     band 0: glyph (+ fold-count chip) ONLY - no name text, no staleness
             dot (v1.1/S5: staleness needs :last-attested-ms, which arrives
             with the D-008 s5 attestation walk - deferred)
     band 1: ONE line `glyph name`
     band 2: title + ONE reading line (compressed two-clock stamp +
             asserter; written-by only when it differs - G2: built from
             entry DATA, never a truncation of body text)
   A move record appends the new-since chip op inline (item 6); a fold
   count appends the fold chip (band 0). Ops are local (x 6 inset,
   line-height rows); the caller derives bounds FROM these ops so the
   hit region equals the painted line region (v1.1 B1, per-band)."
  [entry band {:keys [line-height char-advance fold-count move]}]
  (let [lh      (or line-height 18)
        ca      (or char-advance 8)
        glyph   (get kind-glyph (:entry/kind entry) "•")
        target  (:entry/target entry)
        title   (str glyph " " (or (:display-name target) (:id target)))
        stamp   (compressed-two-clock-stamp entry)
        {:keys [asserted-by written-by]} (:entry/actor entry)
        reading (cond-> stamp
                  asserted-by (str " " middot " " asserted-by)
                  (and written-by (not= written-by asserted-by))
                  (str " " middot " written-by " written-by))
        base    (case (long band)
                  0 [{:text glyph :style :card-title}]
                  1 [{:text title :style :card-title}]
                  [{:text title :style :card-title}
                   {:text reading :style :stamps}])
        placed  (vec (map-indexed
                      (fn [i l] (assoc l :x 6 :y (+ 2 (* i lh)) :size 12))
                      base))
        ;; chips ride the FIRST line, after the widest existing op on that
        ;; row (the clip? ancestor truncates at card-w - trap 9; the full
        ;; reason stays in node DATA, the R-1 truncation-vs-naming ruling).
        first-row-end (fn [ops]
                        (reduce max 0
                                (map (fn [o] (if (= (:y o) 2)
                                               (+ (:x o) (* (count (:text o)) ca))
                                               0))
                                     ops)))]
    (as-> placed ops
      (if (and (= 0 (long band)) fold-count (pos? fold-count))
        (conj ops {:text (str fold-chip-glyph fold-count)
                   :x (+ 6 (first-row-end ops)) :y 2 :size 12
                   :style :stamps :trail-face/fold-chip? true
                   :rgba [0.60 0.60 0.70 1.0]})
        ops)
      (if move
        (conj ops {:text (str move-chip-glyph " " (:reason move))
                   :x (+ 10 (first-row-end ops)) :y 2 :size 12
                   :style :stamps :trail-face/new-since-chip? true
                   :rgba [0.95 0.83 0.45 1.0]})
        ops))))

(defn feed-entry-card
  "WP1 s6 <entry> -> rt-node card, BAND-AWARE (CONTRACT_R2 item 1 / R4/R5):
   closed cards are TYPOGRAPHY - line ops in a clip? node, ZERO rect-fill
   ops at every band (G1; the box appears only on the OPEN surface, a
   separate sibling built by the scene). geom carries :band (0|1|2,
   default 2 - an open card is band 3 via the scene, this builder never
   sees it), optional :fold-count (G5) and :move (item 6 chip).
   Bounds rule (v1.1 B1, PER-BAND): the node bounds equal this band's
   painted line region - width = the widest painted line (clamped to
   card-w, where the clip? child truncates paint too), height = the line
   rows. Bounds legitimately DIFFER across bands; band 0 is the densest.
   Interactive id stays :trail-face/*-namespaced (S5); the address stays
   attached as DATA (item 2 / ledger 1)."
  [entry {:keys [card-w line-height char-advance band] :as geom}]
  (let [lh    (or line-height 18)
        cw    (or card-w 300)
        ca    (or char-advance 8)
        band  (or band 2)
        ops   (band-line-ops entry band geom)
        n-rows (inc (reduce max 0 (map (fn [o] (quot (- (:y o) 2) lh)) ops)))
        text-w (reduce max 0 (map (fn [o] (+ (:x o) (* (count (:text o)) ca))) ops))
        w     (min cw (max (* 2 ca) text-w))
        h     (+ 4 (* n-rows lh))]
    (rt/rt-node [:trail-face/card (entry-key entry)] :feed-card
                {:x 0 :y 0 :w w :h h}
                :clip? true
                :data (cond-> {:trail-face/entry-key (entry-key entry)
                               :trail-face/band band
                               ;; item 2 / ledger 1: address as DATA on every node
                               :trail-face/address (:entry/address entry)
                               :trail-face/click {:action :trail-face/toggle-expand
                                                  :id (entry-key entry)}
                               :trail-face/dead-end? (boolean
                                                      (or (:dead-end? (:entry/detail entry))
                                                          (= :dead-end
                                                             (:kind (:entry/detail entry)))))}
                        (:fold-count geom) (assoc :trail-face/fold-count (:fold-count geom))
                        (:folded geom)     (assoc :trail-face/folded (:folded geom))
                        (:move geom)       (assoc :trail-face/move (:move geom)))
                :children
                [(rt/rt-node [:trail-face/card-text (entry-key entry)] :card-text
                             {:x 0 :y 0 :w w :h h}
                             :text ops)])))

(defn expanded-address-op
  "Card expansion (band 3) renders the EXPANDED element's OWN address as
   the addressable STRING (item 2 / R5 / G1 - the element id itself, never
   the noisy `pr-str` bundle-query map). Tagged :element-address? so it is
   NOT counted as the FACE address (which lives once, in the rim)."
  [element-address]
  {:text (address->addressable-string element-address)
   :style :address
   :trail-face/element-address? true})
