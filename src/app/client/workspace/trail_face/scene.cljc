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

;; --- View-3 text face scene (OP-21, text half) ---------------------------------

(defn build-text-face-scene
  "WP1 render-bundle-text output -> scene tree.
   geom: {:viewport-w :viewport-h :line-height :font-size :char-advance :pad}
   (char-advance is an ARGUMENT - S3). The face's own address renders as
   exactly ONE marked header line (gate 1); the projection body renders
   VERBATIM, marker-colored, never re-wrapped (gate 2/3)."
  [{:keys [text address coverage geom]}]
  (let [{:keys [viewport-w line-height font-size pad]
         :or   {viewport-w 800 line-height 18 font-size 13 pad 8}} geom
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
                         :data {:trail-face/content-h (+ header-h body-h)}
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
        rel-lines (vec (for [[kind es] (sort-by key this-rels), e es]
                         {:text (str "-> " (name kind) " " (get-in e [:to :id])
                                     " by " (:asserted-by e))
                          :style (if (= :retracted (:status e)) :retracted :relation-out)
                          :badges (cards/provenance-badges e)}))
        hole-rows (vec (for [[_ es] this-rels, e es
                             :when (cards/hole-endpoint? (:to e))]
                         e))
        addr-op   (cards/expanded-address-op bundle)
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
                              :clip? true
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
        header-op (-> {:text (pr-str (address-with-order (:feed/address feed) order))
                       :style :address :trail-face/address? true}
                      (assoc :x pad :y 0 :size 13
                             :rgba (tf/style->rgba :address)))
        header-h  (+ line-height 6)
        ;; --- stack cards: y cursor per entry, x by lane ---
        build     (loop [es sorted, y header-h, acc [] , bounds {}]
                    (if (empty? es)
                      {:cards acc :bounds bounds :content-h y}
                      (let [e     (first es)
                            ek    (cards/entry-key e)
                            lane  (get lanes-map ek 0)
                            x     (+ pad (* lane indent-unit))
                            card  (cards/feed-entry-card e geom)
                            card  (assoc card :bounds
                                         (assoc (:bounds card) :x x :y y))
                            tid   (get-in e [:entry/target :id])
                            exp?  (contains? expanded ek)
                            bundle (when exp? (get bundles tid))
                            expn  (when bundle
                                    (-> (expansion-node tid bundle geom)
                                        (update :bounds assoc
                                                :x x :y (+ y (get-in card [:bounds :h])))))
                            h     (+ (get-in card [:bounds :h])
                                     (if expn (get-in expn [:bounds :h]) 0))
                            abs-b {:x x :y y :w card-w
                                   :h (get-in card [:bounds :h])}]
                        (recur (rest es)
                               (+ y h 10)
                               (into acc (if expn [card expn] [card]))
                               (update bounds tid #(or % abs-b))))))
        {:keys [cards bounds content-h]} build
        ;; --- edges: relation-transition details + expanded bundles' L3 :this ---
        entry-edges  (into []
                           (keep (fn [e]
                                   (when (and (= :relation-transition (:entry/kind e))
                                              (get-in e [:entry/detail :from])
                                              (get-in e [:entry/detail :to]))
                                     (:entry/detail e))))
                           sorted)
        bundle-edges (into []
                           (comp (map (fn [tid] (get bundles tid)))
                                 (keep identity)
                                 (mapcat (fn [b] (vals (:bundle/targets b))))
                                 (mapcat (fn [tb] (vals (get-in tb [:relations :this] {}))))
                                 cat)
                           (into #{} (map (fn [ek] (first ek))) expanded))
        edges       (into entry-edges bundle-edges)
        dead-ends   (lanes/dead-end-ids edges)
        conn-rects  (lanes/connectors edges bounds)
        spine-cards (vec (for [c cards
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
                                :trail-face/order order}
                         :children
                         (into
                          [(rt/rt-node :trail-face/address-header :header
                                       {:x 0 :y 0 :w viewport-w :h header-h}
                                       :text [header-op])]
                          (concat conn-nodes
                                  cards
                                  (when omissions
                                    [(update omissions :bounds assoc
                                             :x pad :y content-h)]))))]
    (sanitize-tree coverage root)))

(defn content-height
  "Content height the scroll clamp runs against."
  [scene]
  (get-in scene [:data :trail-face/content-h] 0))
