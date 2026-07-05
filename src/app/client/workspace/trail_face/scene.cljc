(ns app.client.workspace.trail-face.scene
  "Face scene assembly (pure cljc): (data, geometry, view-state) ->
   ONE rt-node tree. The CALLER caches the returned value (cached-scene
   law INV-11 / gate 13): render flattens from it, clicks hit-test the
   SAME object; this ns never touches an atom.
   Scenes are scroll-INDEPENDENT (the camera carries scroll, PLAN s5);
   clamp math is exported for the wheel handler.
   Every emitted text op passes the sanitizer (trap 13: sanitize at op
   emission) when a coverage set is provided."
  (:require [clojure.string :as string]
            [app.client.workspace.rect-tree :as rt]
            [app.client.workspace.trail-face.sanitize :as san]
            [app.client.workspace.trail-face.text-face :as tf]
            [app.client.workspace.trail-face.cards :as cards]
            [app.client.workspace.trail-face.lanes :as lanes]))

;; --- Entry command (OP-44) ------------------------------------------------

(defn parse-trail-command
  "\"/trail ...\" command text -> a trail-face state op, or nil when it is
   not a trail command. PURE: the EDN reader fn is passed IN (S2 - no
   reader conditionals in this directory). Forms:
     /trail text <address-edn>      -> {:op :set :state {:face :text ...}}
     /trail timeline [<address>]    -> {:op :set :state {:face :timeline ...}}
     /trail order claimed|arrival   -> {:op :order :order <kw>}
     /trail off                     -> {:op :off}"
  [s read-edn]
  (let [s (string/trim (or s ""))
        s (if (string/starts-with? s "/") (subs s 1) s)]
    (when (or (= s "trail") (string/starts-with? s "trail "))
      (let [rest-s (string/trim (subs s 5))
            i (string/index-of rest-s " ")
            word (if i (subs rest-s 0 i) rest-s)
            tail (if i (string/trim (subs rest-s (inc i))) "")]
        (cond
          (= word "off")
          {:op :off}

          (= word "order")
          {:op :order :order (keyword (if (seq tail) tail "arrival"))}

          (= word "text")
          {:op :set :state {:face :text :order :arrival
                            :address (when (seq tail) (read-edn tail))}}

          (or (= word "timeline") (= word ""))
          {:op :set :state {:face :timeline :order :arrival
                            :address (when (seq tail) (read-edn tail))}}

          :else nil)))))

;; --- Scroll clamp (OP-22; clamp on next wheel, RETRO s3.8) -------------------

(defn clamp-scroll
  "Clamp a scroll offset against content height. Codepoint/line-based
   content-h is computed by the builders below, never UTF-16 counts."
  [offset content-h viewport-h]
  (max 0 (min offset (max 0 (- content-h viewport-h)))))

;; --- Sanitize pass (applied at emission) --------------------------------------

(defn- sanitize-ops
  [coverage ops]
  (if coverage
    (mapv #(san/sanitize-op coverage san/fallback-cp %) ops)
    ops))

(defn sanitize-tree
  "Walk an rt-node tree, sanitizing every text op codepoint-wise.
   No-op when coverage is nil (JVM tests pass the REAL atlas coverage)."
  [coverage node]
  (if (nil? coverage)
    node
    (-> node
        (update :text (fn [ops] (sanitize-ops coverage (or ops []))))
        (update :children (fn [cs] (mapv #(sanitize-tree coverage %) cs))))))

;; --- Rim v0 (item 3 / R1) -------------------------------------------------------

(defn rim-slots
  "Item 3 / R1 - rim v0: the four fixed chrome slots for a trail face
   (PURE, gate-tested at op level; combined_text positions + colors them
   inside the existing 24px status strip). EXACTLY four slots, in order:
     :scope   land + how much is in view (source count)
     :delta   new-since-last-pull, arrival-clock - labeled AS SUCH (the
              honest interim; attestation delta is a later milestone -
              designer falsification pass #5)
     :address the LIVE face address as an addressable string (item 2 / C2:
              the face address lives HERE, one place, off the card face)
     :palette the palette-reachability hint (C1 - the cmd panel opens over
              the face; ^K toggles it, events.cljs)
   Total over BOTH faces (the timeline carries a feed; the text face does
   not). Editor mode NEVER calls this (combined_text guards on trail-mode?)."
  [{:keys [face feed address now-ms land palette-hint]}]
  (let [land    (or land "softland")
        n       (count (:feed/entries feed))
        arrival (or (:feed/rendered-at-ms feed) now-ms)
        addr    (cards/address->addressable-string
                 (or address (:feed/address feed)))]
    [{:slot :scope :trail-face/rim? true
      :text (case face
              :timeline (str land " " cards/middot " " n
                             " source" (when (not= n 1) "s") " in view")
              (str land " " cards/middot " text face"))}
     {:slot :delta :trail-face/rim? true
      :text (if arrival
              (str "since last pull " cards/middot " arrived "
                   (cards/ms->date-str arrival))
              (str "since last pull " cards/middot " —"))}
     {:slot :address :trail-face/rim? true :trail-face/address? true
      :text addr}
     {:slot :palette :trail-face/rim? true
      :text (or palette-hint "^K palette")}]))

;; --- View-3 text face scene (OP-21, text half) ---------------------------------

(defn build-text-face-scene
  "WP1 render-bundle-text output -> scene tree.
   geom: {:viewport-w :viewport-h :line-height :font-size :char-advance :pad}
   (char-advance is an ARGUMENT - S3). The face's own address renders as
   exactly ONE marked header line (gate 1); the projection body renders
   VERBATIM, marker-colored, never re-wrapped (gate 2/3)."
  [{:keys [text address coverage geom]}]
  (let [{:keys [viewport-w line-height font-size pad now-ms]
         :or   {viewport-w 800 line-height 18 font-size 13 pad 8}} geom
        rim (rim-slots {:face :text :address address :now-ms now-ms})
        {:keys [ops]} (tf/projection->line-ops text)
        header-op (-> (tf/address-header-op address)
                      (assoc :x pad :y 0 :size font-size
                             :rgba (tf/style->rgba :address)))
        body-ops  (mapv (fn [{:keys [line text style]}]
                          {:text text
                           :x pad :y (* line line-height) :size font-size
                           :style style :rgba (tf/style->rgba style)})
                        ops)
        body-h    (* (count ops) line-height)
        header-h  (+ line-height 4)
        root (rt/rt-node :trail-face/text-root :trail-text
                         {:x 0 :y 0 :w viewport-w :h (+ header-h body-h)}
                         :data {:trail-face/content-h (+ header-h body-h)
                                :trail-face/rim-slots rim}
                         :children
                         [(rt/rt-node :trail-face/address-header :header
                                      {:x 0 :y 0 :w viewport-w :h header-h}
                                      :text [header-op])
                          (rt/rt-node :trail-face/text-body :body
                                      {:x 0 :y header-h :w viewport-w :h body-h}
                                      :text body-ops)])]
    (sanitize-tree coverage root)))

;; --- Timeline face scene (OP-21, card half) -------------------------------------

(def lane-gap 24)

(defn address-with-order
  "The face's rendered address carries the ACTIVE order param (face
   law 4: the order param is part of the address). Address is a
   one-line EDN list (trail/<query> {params}); unknown shapes pass
   through untouched (honesty over guessing)."
  [address order]
  (if (and (seq? address) (= 2 (count address)) (map? (second address)))
    (list (first address) (assoc (second address) :order order))
    address))

(defn- connector-style
  [{:keys [kind status]}]
  (let [base (case kind
               :based-on      [0.55 0.75 0.95]
               :produced      [0.60 0.85 0.70]
               :built-over    [0.80 0.70 0.55]
               :new-direction [0.85 0.60 0.85]
               :dead-end      [0.85 0.50 0.50]
               :elaborates    [0.70 0.70 0.85]
               :references    [0.65 0.65 0.65]
               :confirms      [0.55 0.85 0.65]
               :refutes       [0.90 0.55 0.55]
               :supersedes    [0.90 0.75 0.45]
               :lane          [0.35 0.37 0.42]
               [0.6 0.6 0.6])
        alpha (if (= status :retracted) 0.30 0.9)]
    {:bg (conj base alpha)
     :struck? (= status :retracted)}))

(defn- connector-node
  [i {:keys [connector] :as r}]
  (let [{:keys [bg struck?]} (connector-style connector)]
    (rt/rt-node [:trail-face/connector i] :connector
                {:x (:x r) :y (:y r) :w (:w r) :h (:h r)}
                :style {:bg bg}
                :data {:trail-face/connector connector
                       :trail-face/struck? struck?})))

;; --- Kraft marks (item 4 / R6) --------------------------------------------------

(def kraft-tack
  "U+251C BOX DRAWINGS LIGHT VERTICAL AND RIGHT (├) - the covered
   substitute for U+22A2 RIGHT TACK (⊢), which is ABSENT from the merged
   font_atlas.json (trap 5, verified 2026-07-05). ├ is in the design's own
   blessed connector set (⊢ │ ├ └ •) and reads as a branch junction. OI-2
   slug expansion is the real fix; the atlas is NOT regen'd in this package."
  "├")

(def kraft-rgba
  "Kraft/amber - the assertion material tone (R6: mark rows are structurally
   distinct from terrain ink at every band)."
  [0.82 0.68 0.40 1.0])

(defn kraft-label-text
  "R6 band-2 mark label: '<tack> <kind> · <asserter>'. For an off-screen
   line, names the FAR end so the edge never silently vanishes (trap 3)."
  [edge actor far-id]
  (str kraft-tack " " (name (:kind edge))
       (when far-id (str " -> " far-id " (off screen)"))
       (when-let [a (:asserted-by actor)] (str " " cards/middot " " a))))

(defn kraft-mark-nodes
  "Item 4 / R6 / G3: a CLOSED :relation-transition entry -> assertion
   material, NEVER a box-card. Returns a FLAT vector of rt-nodes added
   DIRECTLY to root's children (the connector-node pattern - absolute
   coords, root at 0,0):
     both endpoints in card-bounds -> the Manhattan connector rects (thin,
       retro T-3) + a :kraft-connector label node
       '<tack> <kind> · <asserter>' (band >=2, asserter badge - G3);
     an endpoint absent -> a standalone :kraft-line: a horizontal kraft
       rect at the entry's (x,y) time position + a label node naming the
       FAR end (trap 3 - an edge may never silently vanish)."
  [i entry card-bounds {:keys [x y card-w line-height]}]
  (let [lh      (or line-height 18)
        w       (or card-w 300)
        edge    (:entry/detail entry)
        actor   (:entry/actor entry)
        from-id (get-in edge [:from :id])
        to-id   (get-in edge [:to :id])
        fb      (get card-bounds from-id)
        tb      (get card-bounds to-id)]
    (if (and fb tb)
      ;; both endpoints on screen -> labeled connector (no card fill)
      (let [rects (lanes/connector-rects edge fb tb)
            conn  (vec (map-indexed (fn [j r] (connector-node [i j] r)) rects))
            lx    (+ (min (:x fb) (:x tb)) 8)
            ly    (max 0 (- (min (:y fb) (:y tb)) 2))]
        (conj conn
              ;; clip? + text-in-child (the cards.cljc idiom): a node's own
              ;; text clips only to the INCOMING clip, so the label needs a
              ;; clip? ancestor to truncate at card-w instead of bleeding
              ;; across neighboring lanes (gate-review fix 2026-07-05).
              (rt/rt-node [:trail-face/kraft-connector i] :kraft-connector
                          {:x lx :y ly :w w :h lh}
                          :clip? true
                          :data {:trail-face/kraft? true :trail-face/edge edge}
                          :children
                          [(rt/rt-node [:trail-face/kraft-connector-label i] :kraft-label
                                       {:x 0 :y 0 :w w :h lh}
                                       :text [{:text (kraft-label-text edge actor nil)
                                               :x 0 :y 0 :size 12 :style :kraft
                                               :rgba kraft-rgba :trail-face/kraft-label? true}])])))
      ;; one (or both) endpoints off screen -> standalone kraft LINE naming
      ;; the far end (the edge may never vanish)
      (let [far-id (cond fb to-id tb from-id :else to-id)
            line   {:x x :y (+ y (quot lh 2))
                    :w (min 48 (max 24 (quot w 4))) :h lanes/connector-thickness
                    :connector {:kind (:kind edge) :status (:status edge)
                                :segment :kraft-line}}]
        [(connector-node [i :seg] line)
         ;; clip? + text-in-child: truncate the far-end label at card-w
         ;; (never bleed) while still NAMING the far end (R6).
         (rt/rt-node [:trail-face/kraft-line i] :kraft-line
                     {:x (+ x 56) :y y :w w :h lh}
                     :clip? true
                     :data {:trail-face/kraft? true :trail-face/edge edge
                            :trail-face/off-screen far-id}
                     :children
                     [(rt/rt-node [:trail-face/kraft-line-label i] :kraft-label
                                  {:x 0 :y 0 :w w :h lh}
                                  :text [{:text (kraft-label-text edge actor far-id)
                                          :x 0 :y 2 :size 12 :style :kraft
                                          :rgba kraft-rgba :trail-face/kraft-label? true}])])]))))

(defn kraft-handle-node
  "Item 4: an OPEN :relation-transition renders a typographic HANDLE
   (kraft-toned, NOT a box-card) carrying the toggle-expand click, above its
   detail surface. Closing it returns the relation to a mark."
  [entry {:keys [x y card-w line-height]}]
  (let [ek    (cards/entry-key entry)
        edge  (:entry/detail entry)
        actor (:entry/actor entry)]
    (rt/rt-node [:trail-face/kraft-handle ek] :kraft-handle
                {:x x :y y :w (or card-w 300) :h (or line-height 18)}
                :clip? true
                :data {:trail-face/entry-key ek
                       :trail-face/kraft? true
                       :trail-face/click {:action :trail-face/toggle-expand :id ek}}
                :children
                [(rt/rt-node [:trail-face/kraft-handle-label ek] :kraft-label
                             {:x 0 :y 0 :w (or card-w 300) :h (or line-height 18)}
                             :text [{:text (str (kraft-label-text edge actor nil) "  (open)")
                                     :x 6 :y 2 :size 12 :style :kraft :rgba kraft-rgba
                                     :trail-face/kraft-label? true}])])))

(defn- expansion-node
  "Expanded card content: material preview (CLIPPED - the gate-6
   straddle case), staleness, relations, verdict fold, omissions block,
   and the expanded target's OWN address line (gate 1)."
  [target-id bundle geom]
  (let [{:keys [card-w line-height now-ms]
         :or   {card-w 300 line-height 18}} geom
        tb        (get-in bundle [:bundle/targets target-id])
        stale     (cards/staleness-treatment (:times tb) (or now-ms 0))
        preview   (cards/material-preview (:material tb) 200)
        vlines    (cards/verdict-lines (get-in tb [:verdicts :current]))
        this-rels (get-in tb [:relations :this])
        ;; direction is RELATIVE to the expanded card (first-light fix
        ;; 2026-07-05, G11's sibling at the card level): an INCOMING edge
        ;; printed "-> kind <to>" showed the card's own id as its target —
        ;; a commit that "produced itself". Outgoing (:from = me) -> the far
        ;; end is :to; incoming (:to = me) -> "<-" and the far end is :from.
        ;; The map must not lie about who did what to whom.
        rel-lines (vec (for [[kind es] (sort-by key this-rels), e es]
                         (let [out? (= target-id (get-in e [:from :id]))
                               far  (if out? (get-in e [:to :id]) (get-in e [:from :id]))]
                           {:text (str (if out? "-> " "<- ") (name kind) " " far
                                       " by " (:asserted-by e))
                            :style (if (= :retracted (:status e)) :retracted :relation-out)
                            :badges (cards/provenance-badges e)})))
        hole-rows (vec (for [[_ es] this-rels, e es
                             :when (cards/hole-endpoint? (:to e))]
                         e))
        ;; item 2 / G1: the OPEN card prints the EXPANDED element's OWN
        ;; address (its id) as the addressable string, not the noisy
        ;; multi-target bundle query.
        addr-op   (cards/expanded-address-op (:address tb))
        ;; F-L4: ONE op per line, capped - the renderer breaks embedded
        ;; newlines AFTER the tree walk, so multi-line ops escape both the
        ;; height math and the clip and overprint the info block below.
        preview-line-cap 6
        raw-lines (string/split (:text preview) #"\n" -1)
        shown     (vec (take preview-line-cap raw-lines))
        hidden    (- (count raw-lines) (count shown))
        notice    (when (or (:truncated? preview) (pos? hidden))
                    (str "… "
                         (when (pos? hidden) (str hidden " more lines "))
                         (when (:truncated? preview) (:notice preview))))
        preview-lines (vec (map-indexed
                            (fn [i l] {:text l :x 4 :y (* i line-height) :size 12
                                       :style :material})
                            (cond-> shown notice (conj notice))))
        info-ops  (vec (map-indexed
                        (fn [i {:keys [text style]}]
                          {:text text :x 4 :y (* i line-height) :size 12
                           :style style :rgba (tf/style->rgba style)})
                        (into [{:text (:label stale) :style :stamps}]
                              (concat rel-lines vlines))))
        preview-h (* (count preview-lines) line-height)
        info-h    (* (count info-ops) line-height)
        omissions (cards/omissions-block
                   (into (vec (:bundle/omissions bundle)) (:omissions tb))
                   geom)
        hole-nodes (vec (map-indexed
                         (fn [i r]
                           (-> (cards/hole-endpoint-card r geom)
                               (assoc-in [:bounds :y]
                                         (* i (+ 4 line-height)))))
                         hole-rows))
        holes-h   (* (count hole-nodes) (+ 4 line-height))
        om-h      (if omissions (get-in omissions [:bounds :h]) 0)
        addr-h    line-height
        total-h   (+ preview-h info-h om-h holes-h addr-h 8)]
    ;; NB: the expansion IS clip? — every section's text (material, info /
    ;; relations, holes, omissions, address) truncates at card-w, and the
    ;; fixed-height bg children (holes / omissions) clamp inside it. This is
    ;; safe against the gate-6 straddle case (bg rects above the viewport
    ;; when the expansion is partially scrolled off the top) because
    ;; rect_tree's child-clip now INTERSECTS a clip? node's bounds with the
    ;; ancestor clip instead of replacing it (gate-review fix 2026-07-05) —
    ;; children can escape NEITHER the card width NOR the viewport.
    (rt/rt-node [:trail-face/expansion target-id] :expansion
                {:x 0 :y 0 :w card-w :h total-h}
                :clip? true
                :style {:bg [0.13 0.14 0.17 1.0]}
                :data {:trail-face/expanded target-id
                       :trail-face/staleness (:state stale)}
                :children
                (into
                 [(rt/rt-node [:trail-face/material target-id] :material
                              {:x 0 :y 0 :w card-w :h preview-h}
                              :text preview-lines)
                  (rt/rt-node [:trail-face/expansion-info target-id] :info
                              {:x 0 :y preview-h :w card-w :h info-h}
                              :text info-ops)]
                 (concat
                  (when (seq hole-nodes)
                    [(rt/rt-node [:trail-face/holes target-id] :holes
                                 {:x 0 :y (+ preview-h info-h) :w card-w :h holes-h}
                                 :children hole-nodes)])
                  (when omissions
                    [(assoc-in omissions [:bounds :y] (+ preview-h info-h holes-h))])
                  [(rt/rt-node [:trail-face/expansion-address target-id] :addr
                               {:x 0 :y (+ preview-h info-h holes-h om-h)
                                :w card-w :h addr-h}
                               :text [(assoc addr-op :x 4 :y 0 :size 12
                                             :rgba (tf/style->rgba :address))])])))))

(defn build-timeline-scene
  "WP1 feed (+ pulled bundles for expanded cards) -> scene tree.
   view-state: {:expanded #{entry-key} :order :arrival|:claimed}
   geom: {:viewport-w :line-height :card-w :char-advance :pad :now-ms}.
   Cards thread into lanes; edges render as Manhattan thin rects;
   dead-ends terminate their lane; omissions render as pixels."
  [{:keys [feed bundles view-state coverage geom]}]
  (let [{:keys [viewport-w line-height pad now-ms]
         :or   {viewport-w 800 line-height 18 pad 8}} geom
        ;; F-L5: single-column full-width feed. The lane grid degenerates
        ;; on a corpus of single-entry threads (one lane per doc = the
        ;; staircase); threads read as a small LEFT INDENT instead. The
        ;; real lane/DAG form question is design-track material
        ;; (FIRST_LIGHT ledger), not something to invent here.
        indent-unit 14
        indent-slots 8
        card-w    (max 240 (- viewport-w (* 2 pad) (* indent-unit indent-slots)))
        geom      (assoc geom :card-w card-w :line-height line-height
                         :now-ms now-ms)
        entries   (vec (:feed/entries feed))
        order     (:order view-state :arrival)
        sorted    (vec (sort-by (if (= order :claimed)
                                  (fn [e] [(- (or (:time/claimed-ms e)
                                                  (:time/arrival-ms e)))
                                           (- (:time/arrival-ms e))])
                                  (fn [e] [(- (:time/arrival-ms e))]))
                                entries))
        ;; F-L2: lanes wrap into the bounded indent slots; spines group
        ;; by the UNWRAPPED thread assignment below.
        raw-lanes (lanes/assign-lanes sorted)
        lanes-map (lanes/assign-lanes sorted indent-slots)
        expanded  (:expanded view-state #{})
        mark?     (fn [e] (= :relation-transition (:entry/kind e)))
        ;; item 2 / C2: NO scene-header address op. The FACE address lives
        ;; ONCE, in the constant rim chrome (rim-slots below, rendered by
        ;; combined_text in the status strip) - it does not scroll away.
        rim       (rim-slots {:face :timeline :feed feed :now-ms now-ms
                              :address (address-with-order (:feed/address feed) order)})
        top-pad   (+ pad 4)
        ;; --- stack: y cursor per entry, x by lane ---
        ;; item 4 / R6: :relation-transition entries are MARKS, never
        ;; box-cards. Terrain (source/transcript) entries are cards. A
        ;; CLOSED mark contributes assertion material (connector / kraft
        ;; line); an OPEN mark shows a typographic handle + detail surface.
        build     (loop [es sorted, y top-pad, acc [], bounds {}, marks []]
                    (if (empty? es)
                      {:nodes acc :bounds bounds :marks marks :content-h y}
                      (let [e     (first es)
                            ek    (cards/entry-key e)
                            lane  (get lanes-map ek 0)
                            x     (+ pad (* lane indent-unit))
                            tid   (get-in e [:entry/target :id])
                            exp?  (contains? expanded ek)]
                        (cond
                          ;; OPEN relation-transition: kraft handle + surface
                          (and (mark? e) exp?)
                          (let [bundle (get bundles tid)
                                handle (kraft-handle-node
                                        e {:x x :y y :card-w card-w
                                           :line-height line-height})
                                expn   (when bundle
                                         (-> (expansion-node tid bundle geom)
                                             (update :bounds assoc
                                                     :x x :y (+ y line-height))))
                                h      (+ line-height
                                         (if expn (get-in expn [:bounds :h]) 0))
                                hb     {:x x :y y :w card-w :h line-height}]
                            (recur (rest es) (+ y h 10)
                                   (into acc (if expn [handle expn] [handle]))
                                   (update bounds tid #(or % hb))
                                   marks))
                          ;; CLOSED relation-transition: a MARK, no box-card
                          (mark? e)
                          (recur (rest es) (+ y line-height 6)
                                 acc bounds
                                 (conj marks {:entry e :x x :y y}))
                          ;; terrain entry: a feed card (+ optional surface)
                          :else
                          (let [card  (-> (cards/feed-entry-card e geom)
                                          (update :bounds assoc :x x :y y))
                                bundle (when exp? (get bundles tid))
                                expn  (when bundle
                                        (-> (expansion-node tid bundle geom)
                                            (update :bounds assoc
                                                    :x x :y (+ y (get-in card [:bounds :h])))))
                                h     (+ (get-in card [:bounds :h])
                                         (if expn (get-in expn [:bounds :h]) 0))
                                abs-b {:x x :y y :w card-w
                                       :h (get-in card [:bounds :h])}]
                            (recur (rest es) (+ y h 10)
                                   (into acc (if expn [card expn] [card]))
                                   (update bounds tid #(or % abs-b))
                                   marks))))))
        {:keys [nodes bounds marks content-h]} build
        ;; --- edges ---
        ;; rt-edges: relation-transition feed-entry edges -> kraft marks
        ;; below (item 4). bundle-edges: expanded targets' L3 :this
        ;; relations -> connectors (their off-screen ends are already
        ;; preserved as rel-line text in the expansion, so a dropped
        ;; connector never hides an edge here).
        rt-edges     (into []
                           (keep (fn [e]
                                   (let [d (:entry/detail e)]
                                     (when (and (mark? e) (:from d) (:to d)) d))))
                           sorted)
        bundle-edges (into []
                           (comp (map (fn [tid] (get bundles tid)))
                                 (keep identity)
                                 (mapcat (fn [b] (vals (:bundle/targets b))))
                                 (mapcat (fn [tb] (vals (get-in tb [:relations :this] {}))))
                                 cat)
                           (into #{} (map (fn [ek] (first ek))) expanded))
        dead-ends   (lanes/dead-end-ids (into rt-edges bundle-edges))
        conn-rects  (lanes/connectors bundle-edges bounds)
        ;; item 4 / G3: CLOSED relation-transition entries -> kraft marks
        ;; (labeled connector when both endpoints on screen; standalone
        ;; kraft line naming the far end when one is off screen).
        mark-nodes  (into []
                          cat
                          (map-indexed
                           (fn [i m]
                             (kraft-mark-nodes i (:entry m) bounds
                                               {:x (:x m) :y (:y m)
                                                :card-w card-w
                                                :line-height line-height}))
                           marks))
        spine-cards (vec (for [c nodes
                               :when (= :feed-card (:type c))]
                           {:entry-key (get-in c [:data :trail-face/entry-key])
                            :bounds (:bounds c)
                            :dead-end? (or (get-in c [:data :trail-face/dead-end?])
                                           (contains? dead-ends
                                                      (first (get-in c [:data :trail-face/entry-key]))))}))
        spine-rects (lanes/lane-spines spine-cards raw-lanes)
        conn-nodes  (vec (map-indexed connector-node
                                      (into spine-rects conn-rects)))
        omissions   (cards/omissions-block (:feed/omissions feed)
                                           (assoc geom :card-w (- viewport-w (* 2 pad))))
        om-h        (if omissions (get-in omissions [:bounds :h]) 0)
        total-h     (+ content-h om-h 10)
        root (rt/rt-node :trail-face/timeline-root :trail-timeline
                         {:x 0 :y 0 :w viewport-w :h total-h}
                         :data {:trail-face/content-h total-h
                                :trail-face/order order
                                :trail-face/rim-slots rim}
                         :children
                         (into
                          (into (vec conn-nodes) mark-nodes)
                          (concat nodes
                                  (when omissions
                                    [(update omissions :bounds assoc
                                             :x pad :y content-h)]))))]
    (sanitize-tree coverage root)))

(defn content-height
  "Content height the scroll clamp runs against."
  [scene]
  (get-in scene [:data :trail-face/content-h] 0))
