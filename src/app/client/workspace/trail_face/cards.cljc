(ns app.client.workspace.trail-face.cards
  "Kernel-material card builders (pure cljc). A SEPARATE builder set
   from trail.cljs agent-run cards (trap 10): these render WP1 shapes
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

(defn feed-entry-card
  "WP1 s6 <entry> -> rt-node card: kind glyph + display name + two-clock
   stamp + actor badges + address line. Interactive id is
   :trail-face/*-namespaced (S5)."
  [entry {:keys [card-w line-height now-ms] :as _geom}]
  (let [lh      (or line-height 18)
        w       (or card-w 300)
        glyph   (get kind-glyph (:entry/kind entry) "•")
        target  (:entry/target entry)
        name-ln (str glyph " " (or (:display-name target) (:id target)))
        stamp   (two-clock-stamp entry)
        badges  (provenance-badges (:entry/actor entry))
        badge-ln (str/join (str " " middot " ") (map :text badges))
        addr-ln (pr-str (:entry/address entry))
        lines   (cond-> [{:text name-ln  :style :card-title}
                         {:text stamp    :style :stamps}]
                  (seq badge-ln) (conj {:text badge-ln :style :badges})
                  true           (conj {:text addr-ln  :style :address}))
        h       (+ (* (count lines) lh) 8)]
    ;; F-L5: the card CLIPS its own text (ops live in a child node - a
    ;; node's :clip? applies to children). A 120-char address truncates
    ;; VISUALLY at the card edge; the full text stays in the scene tree
    ;; (hit-test/data intact) and the resolvable address is one click away
    ;; on the expansion / in the face header.
    (rt/rt-node [:trail-face/card (entry-key entry)] :feed-card
                {:x 0 :y 0 :w w :h h}
                :clip? true
                :style {:bg [0.16 0.17 0.20 1.0] :radius 4}
                :data {:trail-face/entry-key (entry-key entry)
                       :trail-face/click {:action :trail-face/toggle-expand
                                          :id (entry-key entry)}
                       :trail-face/dead-end? (boolean
                                              (or (:dead-end? (:entry/detail entry))
                                                  (= :dead-end
                                                     (:kind (:entry/detail entry)))))}
                :children
                [(rt/rt-node [:trail-face/card-text (entry-key entry)] :card-text
                             {:x 0 :y 0 :w w :h h}
                             :text (vec (map-indexed
                                         (fn [i l] (assoc l :x 6 :y (+ 4 (* i lh)) :size 12))
                                         lines)))])))

(defn expanded-address-op
  "Card expansion renders the EXPANDED target's own address (gate 1)."
  [bundle]
  {:text (pr-str (:bundle/address bundle))
   :style :address
   :trail-face/address? true})
