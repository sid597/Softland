(ns app.client.workspace.workshop-playground
  "Workshop playground cuts 1–2 — a PLACE, not a mode (disposable; in-memory).

   Sid walks through a quiet door at the edge of the ordinary canvas into a
   dedicated region of the SAME world, far from every real note. On the shelf
   stand the components of his world in plain words; `Take one` puts a live
   block specimen on the table — it types, it focuses, it moves as ONE body.
   Selection is always visible (glow ring + caret + the inspector naming the
   subject); the corner handle resizes away from an anchored top-left corner;
   the inspector answers every click on the specimen itself. `Variation` and
   `Note` grow siblings and margin notes with visible threads back. Cut 2 adds
   a structure pencil: draw an empty frame on the table, or draw real anatomy
   `:box` parts inside a specimen and drag them to arrange/nest. Nothing asks
   him to test, accept, or promote anything.

   What is reused, not reinvented:
   - furniture and specimens are ASSEMBLIES through face-assembly over the
     face-primitives vocabulary (plus one room-local `:wsp-frame` primitive
     registered only in this namespace's registry VALUE);
   - specimens render through the real anatomy path (form-for-wear over the
     live worn anatomy + expand-parts + apply-data + the grammar-0
     interpreter), so a specimen IS a block-family instance, one container,
     one pickable body — frame, text, caret together;
   - gestures ride the existing scene-descriptor seam (register-action! +
     the pointer machine's threshold continuations — the studio pattern);
   - typing rides ground-edit's pure keystroke grammar (apply-ground-keydown
     + the caret math), driving the SAME :block-caret part a real block
     renders. The durable envelope lane is deliberately NOT engaged: the room
     writes nothing to the land, so a capture-phase key guard keeps room
     typing out of the ordinary birth/edit arteries while the camera stands
     in the room.

   Interaction state (selection, drags, travel) is plain data in atoms here;
   everything visible is material. Reload loses the room's specimens — that
   is the playground's honest lifetime."
  (:require [clojure.string :as str]
            [app.client.workspace.events :as ev]
            [app.client.workspace.face-assembly :as face-assembly]
            [app.client.workspace.face-primitives :as face-primitives]
            [app.client.workspace.ground-edit :as ge]
            [app.client.workspace.rect-tree :as rt]
            [app.client.workspace.scene-runtime :as scene-rt]
            [app.client.workspace.scene-store :as ss]
            [app.shared.anatomy-material :as anatomy-material]
            [app.shared.workshop-playground :as wsp]))

;; ===========================================================================
;; State — refs handed in by ground at install; the room's own plain data
;; ===========================================================================

(defonce ^:private !refs
  ;; {:metrics-fn <fn> :wears-fn <fn> :!camera <atom> :escape! <fn>} — passed
  ;; by ground's install call so this namespace never requires ground (no
  ;; cycle) and never reaches into another namespace's private state.
  (atom nil))

(defonce ^:private !room
  ;; :specimens {id {:id :kind :label :name :x :y :text :caret :style
  ;;                 :parent :variations :size-override :w :h :pad
  ;;                 :line-h :char-advance :candidate-form}}
  ;; :selected — THE visibly selected specimen id (verbs apply only to it)
  ;; :part-selected — [specimen-id part-id] for the structure pencil
  ;; :tool — nil or :draw-frame; changes what a threshold drag means
  ;; :gesture — the live move/resize/draw/part continuation's frozen facts
  ;; :travel — token of the running camera travel (stale frames drop out)
  (atom {:specimens {} :order [] :selected nil :part-selected nil
         :counter 0 :part-counter 0 :tool nil
         :gesture nil :travel nil :return-camera nil :door-in? nil}))

(defonce ^:private !compiled
  ;; [anatomy-revision-id master-id style] → {:parts :compiled}. Style is part
  ;; of the key because the frame part bakes the inspector's choices into the
  ;; compiled plan; bounded like the ground's anatomy cache.
  (atom {}))

(declare rebuild-specimen! render-overlays! render-inspector!
         render-threads-for! place-door! ensure-room! render-pencil!
         render-draft! select-at!)

;; ===========================================================================
;; Camera math (screen = world·zoom + pan) + shared lookups
;; ===========================================================================

(defn- screen->world [{:keys [x y zoom]} sx sy]
  (let [z (if (and (number? zoom) (pos? zoom)) zoom 1.0)]
    [(/ (- sx (or x 0.0)) z) (/ (- sy (or y 0.0)) z)]))

(defn- base-metrics []
  (if-let [f (:metrics-fn @!refs)]
    (f)
    {:font-size 19 :char-advance 10.64 :line-h 27
     :viewport {:width 1200 :height 800}}))

(defn- room-active?
  "The room is active while the VIEWPORT CENTER stands inside it — geometric,
   so wandering off by hand releases the typing guard on its own."
  []
  (when-let [refs @!refs]
    (let [vp (:viewport (base-metrics))
          vw (or (:width vp) 1200.0)
          vh (or (:height vp) 800.0)
          [wx wy] (screen->world @(:!camera refs) (/ vw 2.0) (/ vh 2.0))]
      (wsp/in-room? wx wy))))

(defn- selected-specimen []
  (when-let [id (:selected @!room)]
    (get-in @!room [:specimens id])))

(defn- drawing? [] (= :draw-frame (:tool @!room)))

(defn- selected-part?
  [spec-id part-id]
  (= [spec-id part-id] (:part-selected @!room)))

(defn- frame-rect
  "The specimen's VISIBLE frame in world coords (the block's own pad-offset
   geometry: the frame wraps the text grid by `pad` on every side)."
  [spec]
  (when (and (:w spec) (:h spec))
    (let [pad (or (:pad spec) 0.0)]
      {:x (- (:x spec) pad) :y (- (:y spec) pad)
       :w (:w spec) :h (:h spec)})))

(defn- clear-ground-press!
  "A tap on room furniture still resolves the space's tap row afterwards
   (:anchor/place). One microtask later — before any paint — the ordinary
   escape clears that anchor so no stray caret bar stands and no later
   keystroke can birth a note where the finger happened to land."
  []
  (when-let [esc (:escape! @!refs)]
    (js/queueMicrotask esc)))

(defn- tap? [ctx] (nil? (:continuation ctx)))

;; ===========================================================================
;; Slot plumbing — the studio's upsert-or-register pattern
;; ===========================================================================

(defn- upsert!
  [vi tree {:keys [x y scale layer meta] :or {scale 1.0}}]
  (if-let [slot (ss/slot (scene-rt/store-snapshot) vi)]
    (do
      (swap! scene-rt/!scene-store ss/upsert-slot vi
             {:tree tree :container (:container slot)
              :meta (:meta slot) :stratum (:stratum slot)
              :pre-resolved? true})
      (scene-rt/set-transform! (:container slot) {:x x :y y :scale scale}))
    (scene-rt/register-face-instance!
     vi tree {:x x :y y :scale scale :layer layer
              :meta meta :pre-resolved? true})))

(defn- close! [vi] (scene-rt/close-instance! vi))

(defn- apply-tree [compiled vi address data w]
  (face-assembly/apply-assembly
   compiled data
   {:view-instance vi :address address
    :geom {:content-w w :font-size 14.0 :char-advance 7.84
           :line-height 20.0}}))

;; ===========================================================================
;; The room's furniture assemblies — material, plain words only
;; ===========================================================================

(def ^:private ink [0.88 0.90 0.95 1.0])
(def ^:private dim-ink [0.52 0.56 0.64 0.95])
(def ^:private accent-ink [0.95 0.78 0.35 1.0])
(def ^:private panel-bg [0.075 0.09 0.115 0.97])
(def ^:private panel-border [0.30 0.35 0.45 0.9])

(def ^:private door-enter-assembly
  {:assembly/name "wsp-door-enter"
   :assembly/grammar 0
   :root
   {:prim :box
    :props {:w 176.0 :padding [10.0 14.0] :gap 2.0
            :bg [0.10 0.12 0.16 0.94]
            :border-width 1.0 :border-color [0.42 0.48 0.62 0.9]
            :radius 9.0}
    :children
    [{:prim :text-run
      :props {:value "Workshop →" :w 148.0 :size 15.0 :color ink}}]}})

(def ^:private door-exit-assembly
  {:assembly/name "wsp-door-exit"
   :assembly/grammar 0
   :root
   {:prim :box
    :props {:w 176.0 :padding [10.0 14.0] :gap 2.0
            :bg [0.10 0.12 0.16 0.94]
            :border-width 1.0 :border-color [0.42 0.48 0.62 0.9]
            :radius 9.0}
    :children
    [{:prim :text-run
      :props {:value "← Back" :w 148.0 :size 15.0 :color ink}}]}})

(def ^:private floor-assembly
  {:assembly/name "wsp-floor"
   :assembly/grammar 0
   :root
   {:prim :box
    :props {:w (:w wsp/floor-rect) :h (:h wsp/floor-rect)
            :padding [26.0 30.0] :gap 6.0
            :bg [0.055 0.065 0.085 1.0]
            :border-width 1.5 :border-color [0.22 0.26 0.34 0.9]
            :radius 18.0}
    :children
    [{:prim :text-run
      :props {:value "Workshop" :w 420.0 :size 24.0
              :color [0.55 0.60 0.70 1.0]}}
     {:prim :text-run
      :props {:value "Take a block from the shelf. Everything on this table is yours to bend — nothing here touches your notes."
              :w 760.0 :size 13.0 :color [0.42 0.46 0.55 0.95]}}]}})

(defn- shelf-assembly [rows]
  {:assembly/name "wsp-shelf"
   :assembly/grammar 0
   :root
   {:prim :box
    :props {:w wsp/shelf-w :padding [14.0 16.0] :gap 6.0
            :bg panel-bg :border-width 1.0 :border-color panel-border
            :radius 10.0}
    :children
    (into [{:prim :text-run
            :props {:value "Shelf" :w 320.0 :size 16.0 :color ink}}
           {:prim :text-run
            :props {:value "take a block to the table" :w 320.0 :size 12.0
                    :color dim-ink}}]
          (map (fn [{:keys [label take?]}]
                 {:prim :stack
                  :props {:direction :row :gap 10.0 :padding [4.0 0.0]}
                  :children
                  [{:prim :text-run
                    :props {:value label :w 170.0 :size 14.0 :color ink}}
                   {:prim :text-run
                    :props {:value (if take? "Take one" "worn by every block")
                            :w 150.0 :size (if take? 13.0 12.0)
                            :color (if take? accent-ink dim-ink)}}]}))
          rows)}})

(defn- inspector-assembly [title rows]
  {:assembly/name "wsp-inspector"
   :assembly/grammar 0
   :root
   {:prim :box
    :props {:w wsp/inspector-w :padding [14.0 16.0] :gap 6.0
            :bg panel-bg :border-width 1.0 :border-color panel-border
            :radius 10.0}
    :children
    (-> [{:prim :text-run
          :props {:value title :w 344.0 :size 15.0 :color ink}}]
        (into (map (fn [{:keys [text swatch]}]
                     {:prim :stack
                      :props {:direction :row :gap 10.0 :padding [3.0 0.0]}
                      :children
                      (into [{:prim :text-run
                              :props {:value text :w 300.0 :size 14.0
                                      :color ink}}]
                            (when swatch
                              [{:prim :box
                                :props {:w 14.0 :h 14.0 :radius 4.0
                                        :bg swatch :border-width 1.0
                                        :border-color [0.10 0.10 0.12 1.0]}}]))})
                   rows))
        (conj {:prim :text-run
               :props {:value "Click a row to change it. Changes land instantly."
                       :w 344.0 :size 12.0 :color dim-ink}}))}})

(def ^:private ring-assembly
  ;; the unmistakable I-HAVE-IT glow around the selected specimen; carries no
  ;; address so every click inside it falls through to the body below
  {:assembly/name "wsp-ring"
   :assembly/grammar 0
   :root
   {:prim :box
    :props {:w {:bind [:w]} :h {:bind [:h]} :radius {:bind [:radius]}
            :border-width 2.5 :border-color [0.95 0.78 0.35 0.95]
            :bg [0.95 0.78 0.35 0.05]}}})

(def ^:private handle-assembly
  {:assembly/name "wsp-handle"
   :assembly/grammar 0
   :root
   {:prim :box
    :props {:w 16.0 :h 16.0 :radius 4.0
            :bg [0.95 0.78 0.35 0.95]
            :border-width 1.0 :border-color [0.10 0.11 0.14 1.0]}}})

(defn- pencil-assembly [active?]
  {:assembly/name (if active? "wsp-pencil-on" "wsp-pencil-off")
   :assembly/grammar 0
   :root
   {:prim :box
    :props {:w 410.0 :padding [10.0 12.0] :gap 6.0
            :bg panel-bg :border-width 1.0
            :border-color (if active? accent-ink panel-border)
            :radius 10.0}
    :children
    [{:prim :box
      :props {:w 132.0 :padding [6.0 10.0] :radius 7.0
              :bg (if active? [0.33 0.25 0.08 0.98]
                    [0.13 0.16 0.22 0.97])
              :border-width 1.0
              :border-color (if active? accent-ink [0.50 0.56 0.68 0.9])}
      :children [{:prim :text-run
                  :props {:value (if active? "Drawing frame" "Draw frame")
                          :w 108.0 :size 13.0 :color ink}}]}
     {:prim :text-run
      :props {:value (if active?
                       "drag on the table or inside a block · click again or Esc to stop"
                       "outline first, details later")
              :w 382.0 :size 12.0 :color dim-ink}}]}})

(def ^:private draft-assembly
  {:assembly/name "wsp-draft-frame"
   :assembly/grammar 0
   :root
   {:prim :box
    :props {:w {:bind [:w]} :h {:bind [:h]} :radius 5.0
            :bg [0.95 0.78 0.35 0.08]
            :border-width 2.0 :border-color accent-ink}}})

(def ^:private verbs-assembly
  {:assembly/name "wsp-verbs"
   :assembly/grammar 0
   :root
   {:prim :stack
    :props {:direction :row :gap 8.0}
    :children
    [{:prim :box
      :props {:w 92.0 :padding [6.0 12.0] :radius 7.0
              :bg [0.13 0.16 0.22 0.97]
              :border-width 1.0 :border-color [0.50 0.56 0.68 0.9]}
      :children [{:prim :text-run
                  :props {:value "Variation" :w 68.0 :size 13.0 :color ink}}]}
     {:prim :box
      :props {:w 62.0 :padding [6.0 12.0] :radius 7.0
              :bg [0.13 0.16 0.22 0.97]
              :border-width 1.0 :border-color [0.50 0.56 0.68 0.9]}
      :children [{:prim :text-run
                  :props {:value "Note" :w 38.0 :size 13.0 :color ink}}]}]}})

(def ^:private thread-assembly
  ;; one thin segment of the visible thread between related bodies
  {:assembly/name "wsp-thread"
   :assembly/grammar 0
   :root {:prim :box
          :props {:w {:bind [:w]} :h {:bind [:h]}
                  :bg [0.48 0.54 0.66 0.55]}}})

(def ^:private compiled-door-enter
  (face-assembly/compile-assembly face-primitives/registry door-enter-assembly))
(def ^:private compiled-door-exit
  (face-assembly/compile-assembly face-primitives/registry door-exit-assembly))
(def ^:private compiled-floor
  (face-assembly/compile-assembly face-primitives/registry floor-assembly))
(def ^:private compiled-shelf
  (face-assembly/compile-assembly face-primitives/registry
                                  (shelf-assembly wsp/shelf-rows)))
(def ^:private compiled-ring
  (face-assembly/compile-assembly face-primitives/registry ring-assembly))
(def ^:private compiled-handle
  (face-assembly/compile-assembly face-primitives/registry handle-assembly))
(def ^:private compiled-pencil-off
  (face-assembly/compile-assembly face-primitives/registry
                                  (pencil-assembly false)))
(def ^:private compiled-pencil-on
  (face-assembly/compile-assembly face-primitives/registry
                                  (pencil-assembly true)))
(def ^:private compiled-draft
  (face-assembly/compile-assembly face-primitives/registry draft-assembly))
(def ^:private compiled-verbs
  (face-assembly/compile-assembly face-primitives/registry verbs-assembly))
(def ^:private compiled-thread
  (face-assembly/compile-assembly face-primitives/registry thread-assembly))

(def ^:private door-enter-tree
  (assoc-in (apply-tree compiled-door-enter [:wsp-b-panel :door] [:wsp :door]
                        {} 176.0)
            [:data :actions] {:action :wsp/enter}))

(def ^:private door-exit-tree
  (assoc-in (apply-tree compiled-door-exit [:wsp-b-panel :door] [:wsp :door]
                        {} 176.0)
            [:data :actions] {:action :wsp/exit}))

(defn- floor-tree []
  (assoc-in (apply-tree compiled-floor [:wsp-a-floor] [:wsp :floor]
                        {} (:w wsp/floor-rect))
            [:data :actions]
            (cond-> {:action :wsp/floor}
              (drawing?)
              (assoc :binding/gesture :pointer/press
                     :binding/phase :threshold
                     :binding/modifiers #{}))))

(def ^:private shelf-tree
  ;; whole take-able rows are the click target (generous, Fitts-friendly);
  ;; rows sit after the two header lines, hence the +2 child offset
  (reduce
   (fn [t [i {:keys [id take?]}]]
     (if take?
       (-> t
           (assoc-in [:children (+ 2 i) :data :address] [:wsp :shelf id])
           (assoc-in [:children (+ 2 i) :data :actions]
                     {:action :wsp/take :wsp/master id}))
       t))
   (assoc-in (apply-tree compiled-shelf [:wsp-b-panel :shelf] [:wsp :shelf]
                         {} wsp/shelf-w)
             [:data :actions] {:action :wsp/noop})
   (map-indexed vector wsp/shelf-rows)))

(def ^:private handle-tree
  (assoc-in (apply-tree compiled-handle [:wsp-e-over :handle] [:wsp :handle]
                        {} 16.0)
            [:data :actions]
            {:action :wsp/resize
             :binding/gesture :pointer/press
             :binding/phase :threshold
             :binding/modifiers #{}}))

(defn- pencil-tree []
  (let [compiled (if (drawing?) compiled-pencil-on compiled-pencil-off)]
    (-> (apply-tree compiled [:wsp-b-panel :pencil] [:wsp :pencil]
                    {} 410.0)
        (assoc-in [:data :actions] {:action :wsp/noop})
        (assoc-in [:children 0 :data :address] [:wsp :pencil :toggle])
        (assoc-in [:children 0 :data :actions] {:action :wsp/pencil}))))

(def ^:private verbs-tree
  (-> (apply-tree compiled-verbs [:wsp-e-over :verbs] nil {} 200.0)
      (assoc-in [:data :address] [:wsp :verbs])
      (assoc-in [:data :actions] {:action :wsp/noop})
      (assoc-in [:children 0 :data :address] [:wsp :verb :variation])
      (assoc-in [:children 0 :data :actions]
                {:action :wsp/verb :wsp/verb :variation})
      (assoc-in [:children 1 :data :address] [:wsp :verb :note])
      (assoc-in [:children 1 :data :actions]
                {:action :wsp/verb :wsp/verb :note})))

(defn- ring-tree [w h radius]
  (apply-tree compiled-ring [:wsp-e-over :ring] nil
              {:w w :h h :radius radius} w))

(defn- thread-tree [w h]
  (apply-tree compiled-thread nil nil {:w w :h h} w))

(defn- draft-tree [w h]
  (apply-tree compiled-draft [:wsp-g-draft] nil {:w w :h h} w))

;; ===========================================================================
;; Specimen rendering — the real anatomy path over the live worn anatomy
;; ===========================================================================

(defn- frame-prim
  "`:wsp-frame` — the specimen's always-visible frame. Registered only in the
   room's registry VALUE below; the shared vocabulary never learns it. Same
   pad-offset geometry as the block's attention box, plus the inspector's
   radius/fill/frame choices."
  [_ctx {:keys [w h pad radius border-width border-color bg]
         :or {w 0.0 h 0.0 pad 0.0 radius 0.0 border-width 1.0}}
   _children]
  (rt/rt-node :wsp-frame :rect
              {:x (- pad) :y (- pad) :w w :h h}
              :style {:radius radius
                      :border-width border-width
                      :border-color (or border-color [0.50 0.56 0.68 0.95])
                      :bg (or bg [0.0 0.0 0.0 0.0])}))

(defn- room-root-prim
  "The ordinary block root, with its BOUNDS grown to the resized frame when a
   :size-override stands — so the pickable body and the visible frame stay
   ONE rect (the two-body failure is the exact thing this room exists to
   bury)."
  [ctx props children]
  (let [node (face-primitives/block-root-prim ctx props children)
        fw (:frame-w props)
        fh (:frame-h props)]
    (cond-> node
      (and (number? fw) (number? fh))
      (update :bounds
              (fn [b] (assoc b :w (max (:w b) fw) :h (max (:h b) fh)))))))

(defn- room-box-prim
  "The ordinary grammar `:box`, taught only in this room to honor literal x/y
   and publish a part-drag descriptor when the row came from the structure
   pencil. The anatomy row remains a normal validated `:box` row."
  [ctx props children]
  (let [node (face-primitives/box-prim ctx props children)
        spec-id (:wsp-spec-id props)
        part-id (:wsp-part-id props)
        structure? (true? (:wsp-structure? props))]
    (cond-> (update node :bounds merge
                    {:x (double (or (:x props) 0.0))
                     :y (double (or (:y props) 0.0))})
      (and structure? (selected-part? spec-id part-id))
      (update :style merge {:border-width 2.5 :border-color accent-ink
                            :bg [0.95 0.78 0.35 0.10]})

      structure?
      (assoc :data
             {:address [:wsp :part spec-id part-id]
              :actions {:action :wsp/part-drag
                        :wsp/id spec-id :wsp/part-id part-id
                        :binding/gesture :pointer/press
                        :binding/phase :threshold
                        :binding/modifiers #{}}}))))

(def ^:private room-registry
  (assoc face-primitives/registry
         :wsp-frame frame-prim
         :box room-box-prim
         :block-root room-root-prim))

(defn- room-form
  "The specimen's anatomy: the LIVE worn anatomy form (form-for-wear — the
   same recovery the studio's draft birth uses) plus the room's frame part,
   with the root taught to carry the resized frame bounds."
  [anatomy-wear style candidate-form]
  (update (or candidate-form (anatomy-material/form-for-wear anatomy-wear))
          :anatomy/parts
          (fn [parts]
            (conj (mapv (fn [p]
                          (if (= :block-root (:part/prim p))
                            (update p :part/props merge
                                    {:frame-w [:view :block-w]
                                     :frame-h [:view :block-h]
                                     :pad (wsp/pad-of style)})
                            p))
                        (vec (remove #(= :wsp-frame (:part/id %)) parts)))
                  (wsp/frame-part style)))))

(defn- compiled-room-anatomy [anatomy-wear style candidate-form]
  (let [k [(:facet-master/revision-id anatomy-wear)
           (:facet-master/id anatomy-wear)
           style candidate-form]]
    (or (get @!compiled k)
        (let [form (room-form anatomy-wear style candidate-form)
              entry {:parts (anatomy-material/expand-parts
                             (:anatomy/parts form) (:anatomy/defs form))
                     :compiled (face-assembly/compile-assembly
                                room-registry
                                (anatomy-material/assembly-for form))}]
          (when (>= (count @!compiled) 24) (reset! !compiled {}))
          (swap! !compiled assoc k entry)
          entry))))

(defn- specimen-view
  "The lean instance view-model: same closed :view vocabulary the block
   renderer feeds, minus everything a table specimen cannot have (machine
   headers, marks, folds, group selection)."
  [spec m pad selected?]
  (let [text (or (:text spec) "")
        lines (str/split text #"\n" -1)
        n (count lines)
        max-len (reduce max 1 (map count lines))
        natural-w (+ (* max-len (:char-advance m)) (* 2 pad))
        natural-h (+ (* n (:line-h m)) (* 2 pad))
        w (max natural-w (double (or (get-in spec [:size-override :w]) 0.0)))
        h (max natural-h (double (or (get-in spec [:size-override :h]) 0.0)))
        caret-lc (when selected?
                   (ge/caret->line-col text (:caret spec)))]
    {:text text :wrap-col nil :headers [] :header-count 0
     :machine? false :user? true :hover? false
     :focused? (boolean selected?)
     :caret-line (:line caret-lc) :caret-col (:col caret-lc)
     :notice nil :refusal nil :boundary? false :gsel? false
     :sel-spans [] :fold-headers []
     :gold-mark-text nil :gold-mark-count 0
     :silver-mark-text nil :silver-mark-count 0
     :line-count n :max-len max-len
     :block-w w :block-h h
     :font-size (:font-size m) :char-advance (:char-advance m)
     :line-h (:line-h m)
     :placement-derived? false}))

(defn- spec-vi [id] [:wsp-d-spec id])

(defn- specimen-tree
  "Build one specimen's rt-tree through the anatomy path and stamp the whole
   body with the room's move descriptor (tap = select + caret; past the
   threshold = pick it up)."
  [spec]
  (let [refs @!refs
        wears ((:wears-fn refs))
        style (:style spec)
        m (wsp/text-metrics (base-metrics) style)
        pad (wsp/pad-of style)
        {:keys [parts compiled]} (compiled-room-anatomy
                                  (:anatomy wears) style
                                  (:candidate-form spec))
        view (specimen-view spec m pad (= (:id spec) (:selected @!room)))
        data (anatomy-material/apply-data parts wears view (:id spec))
        vp (:viewport (base-metrics))
        tree (face-assembly/apply-assembly
              compiled data
              {:view-instance (spec-vi (:id spec))
               :address (:id spec)
               :geom {:viewport-w (or (:width vp) 1200)
                      :viewport-h (or (:height vp) 800)
                      :content-w (:block-w view)
                      :font-size (:font-size m)
                      :char-advance (:char-advance m)
                      :line-height (:line-h m)}})]
    {:view view
     :tree (assoc-in tree [:data :actions]
                     {:action :wsp/drag
                      :wsp/id (:id spec)
                      :binding/gesture :pointer/press
                      :binding/phase :threshold
                      :binding/modifiers #{}})}))

(defn- rebuild-specimen! [id]
  (when-let [spec (get-in @!room [:specimens id])]
    (let [{:keys [tree view]} (specimen-tree spec)]
      (swap! !room update-in [:specimens id] assoc
             :w (:block-w view) :h (:block-h view)
             :pad (wsp/pad-of (:style spec))
             :line-h (:line-h view) :char-advance (:char-advance view))
      (upsert! (spec-vi id) tree
               {:x (:x spec) :y (:y spec)
                :layer (if (= :note (:kind spec)) 6 5)
                :meta {:wsp-specimen id}})
      (render-threads-for! id)
      (when (= id (:selected @!room)) (render-overlays!)))))

;; ===========================================================================
;; Overlays — glow ring, corner handle, verb chips (selection made visible)
;; ===========================================================================

(defn- close-overlays! []
  (close! [:wsp-e-over :ring])
  (close! [:wsp-e-over :handle])
  (close! [:wsp-e-over :verbs]))

(defn- render-overlays! []
  (let [sel (selected-specimen)
        fr (and sel (frame-rect sel))]
    (if-not fr
      (close-overlays!)
      (let [{:keys [x y w h]} fr
            radius (+ (wsp/corner-of (:style sel)) 4.0)]
        (upsert! [:wsp-e-over :ring]
                 (ring-tree (+ w 12.0) (+ h 12.0) radius)
                 {:x (- x 6.0) :y (- y 6.0) :layer 7 :meta {:wsp-ring? true}})
        (upsert! [:wsp-e-over :handle] handle-tree
                 {:x (+ x w -8.0) :y (+ y h -8.0) :layer 8
                  :meta {:wsp-handle? true}})
        (if (= :specimen (:kind sel))
          (upsert! [:wsp-e-over :verbs] verbs-tree
                   {:x x :y (- y 52.0) :layer 8 :meta {:wsp-verbs? true}})
          (close! [:wsp-e-over :verbs]))))))

(defn- move-overlays!
  "Mid-drag the overlay TREES are unchanged; only their containers move
   (transform, never re-shape — the land's own drag discipline)."
  []
  (when-let [fr (some-> (selected-specimen) frame-rect)]
    (let [{:keys [x y w h]} fr]
      (doseq [[vi tx ty] [[[:wsp-e-over :ring] (- x 6.0) (- y 6.0)]
                          [[:wsp-e-over :handle] (+ x w -8.0) (+ y h -8.0)]
                          [[:wsp-e-over :verbs] x (- y 52.0)]]]
        (when-let [slot (ss/slot (scene-rt/store-snapshot) vi)]
          (scene-rt/set-transform! (:container slot) {:x tx :y ty}))))))

;; ===========================================================================
;; Threads — the visible line back to what a thing came from
;; ===========================================================================

(defn- render-thread! [pid cid]
  (let [specs (:specimens @!room)
        pf (some-> (get specs pid) frame-rect)
        cf (some-> (get specs cid) frame-rect)]
    (when (and pf cf)
      (doseq [[k seg] (map vector [:h :v] (wsp/connector-segments pf cf))]
        (upsert! [:wsp-c-thread cid k]
                 (thread-tree (:w seg) (:h seg))
                 {:x (:x seg) :y (:y seg) :layer 3
                  :meta {:wsp-thread cid}})))))

(defn- render-threads-for!
  "Redraw every thread touching `id` (as parent or as child)."
  [id]
  (doseq [[pid cid] (keep (fn [[cid* s]]
                            (when (and (:parent s)
                                       (or (= cid* id) (= (:parent s) id)))
                              [(:parent s) cid*]))
                          (:specimens @!room))]
    (render-thread! pid cid)))

;; ===========================================================================
;; Inspector — the subject named, its appearance in plain words
;; ===========================================================================

(defn- render-inspector! []
  (if-let [sel (selected-specimen)]
    (let [rows (wsp/inspector-rows (:style sel))
          compiled (face-assembly/compile-assembly
                    face-primitives/registry
                    (inspector-assembly (:name sel) rows))
          tree (reduce
                (fn [t [i {:keys [prop]}]]
                  (-> t
                      (assoc-in [:children (inc i) :data :address]
                                [:wsp :inspect prop])
                      (assoc-in [:children (inc i) :data :actions]
                                {:action :wsp/inspect :wsp/prop prop})))
                (assoc-in (apply-tree compiled [:wsp-b-panel :inspector]
                                      [:wsp :inspector] {} wsp/inspector-w)
                          [:data :actions] {:action :wsp/noop})
                (map-indexed vector rows))]
      (upsert! [:wsp-b-panel :inspector] tree
               {:x (:x wsp/inspector-pos) :y (:y wsp/inspector-pos)
                :layer 4 :meta {:wsp-inspector? true}}))
    (close! [:wsp-b-panel :inspector])))

;; ===========================================================================
;; Selection + specimen lifecycle (plain data; visuals follow)
;; ===========================================================================

(defn- select-at!
  "Make `id` THE selected specimen and drop the caret where the finger
   touched (the recorded press point; the same monospace math as the ground's
   caret click)."
  [id]
  (when-let [spec (get-in @!room [:specimens id])]
    (let [prev (:selected @!room)
          [wx wy] (or (:world-point (scene-rt/last-pick))
                      [(:x spec) (:y spec)])
          lh (or (:line-h spec) 22)
          ca (or (:char-advance spec) 9.0)
          line (js/Math.floor (/ (- wy (:y spec)) lh))
          col (js/Math.round (/ (- wx (:x spec)) ca))
          caret (ge/line-col->caret (or (:text spec) "")
                                    (max 0 line) (max 0 col))]
      (swap! !room #(-> %
                        (assoc :selected id)
                        (assoc :part-selected nil)
                        (assoc-in [:specimens id :caret] caret)))
      (when (and prev (not= prev id)) (rebuild-specimen! prev))
      (rebuild-specimen! id)
      (render-inspector!))))

(defn- deselect! []
  (when-let [prev (:selected @!room)]
    (swap! !room assoc :selected nil :part-selected nil)
    (rebuild-specimen! prev))
  (close-overlays!)
  (render-inspector!))

(defn- adopt-specimen!
  "Land a freshly born specimen/note/variation: record it, select it, render
   it (the previous selection visibly lets go first)."
  [spec]
  (let [prev (:selected @!room)]
    (swap! !room #(-> %
                      (assoc :counter (:n spec) :selected (:id spec)
                             :part-selected nil)
                      (assoc-in [:specimens (:id spec)] (dissoc spec :n))
                      (update :order conj (:id spec))))
    (when (and prev (not= prev (:id spec))) (rebuild-specimen! prev))
    (rebuild-specimen! (:id spec))
    (render-inspector!)))

(defn- take-one! [_master-id]
  (let [n (inc (:counter @!room))
        label (wsp/specimen-label n)
        pos (wsp/next-specimen-position (dec n))
        text "Type here"]
    (adopt-specimen!
     {:n n :id (str "wsp-" n) :kind :specimen
      :label label :name (wsp/specimen-name label)
      :x (:x pos) :y (:y pos)
      :text text :caret (count text)
      :style wsp/specimen-style-defaults
      :parent nil :variations 0})))

(defn- variation!
  "A sibling beside the original: a copy of its words and current styling,
   40px to the right, with a visible thread back — and it is the one now
   held."
  []
  (when-let [sel (selected-specimen)]
    (when (= :specimen (:kind sel))
      (let [n (inc (:counter @!room))
            label (wsp/variation-label (:label sel) (:variations sel 0))]
        (swap! !room update-in [:specimens (:id sel) :variations]
               (fnil inc 0))
        (adopt-specimen!
         (assoc sel
                :n n :id (str "wsp-" n)
                :label label :name (wsp/specimen-name label)
                :x (+ (:x sel) (or (:w sel) 240.0) 40.0) :y (:y sel)
                :style (wsp/inherited-style (:style sel))
                :parent (:id sel) :variations 0
                :caret (count (or (:text sel) ""))))))))

(defn- note!
  "A margin note beside the selected specimen: a narrower, muted, typable
   body with a thin thread to what it annotates. Born empty and focused —
   the first keystroke is already ink."
  []
  (when-let [sel (selected-specimen)]
    (when (= :specimen (:kind sel))
      (let [n (inc (:counter @!room))
            note-index (count
                        (filter #(and (= :note (:kind %))
                                      (= (:id sel) (:parent %)))
                                (vals (:specimens @!room))))
            pos (wsp/note-position sel note-index)]
        (adopt-specimen!
         {:n n :id (str "wsp-" n) :kind :note
          :label (str "note " n) :name (wsp/note-name (:label sel))
          :x (:x pos) :y (:y pos)
          :text "" :caret 0
          :style wsp/note-style-defaults
          :size-override {:w 220.0 :h 64.0}
          :parent (:id sel) :variations 0})))))

;; ===========================================================================
;; Structure pencil — candidate anatomy, never a parallel decorative tree
;; ===========================================================================

(defn- canonical-form-for
  [spec]
  (or (:candidate-form spec)
      (let [wears-fn (:wears-fn @!refs)
            wears (when wears-fn (wears-fn))]
        (some-> (:anatomy wears) anatomy-material/form-for-wear))))

(defn- candidate-result
  [form edit]
  (let [result (when form (anatomy-material/edit-candidate form edit))]
    (when (= :candidate (:status result)) result)))

(defn- install-candidate!
  [spec-id result]
  (when-let [form (:form result)]
    (swap! !room assoc-in [:specimens spec-id :candidate-form] form)
    (rebuild-specimen! spec-id)
    true))

(defn- structure-locations
  [form]
  (into []
        (concat
         (keep-indexed
          (fn [i row]
            (when (wsp/structure-part? row)
              {:where :parts :index i :row row}))
          (:anatomy/parts form))
         (mapcat
          (fn [[def-name rows]]
            (keep-indexed
             (fn [i row]
               (when (wsp/structure-part? row)
                 {:where :def :def-name def-name :index i :row row}))
             rows))
          (:anatomy/defs form)))))

(defn- structure-location
  [form part-id]
  (some #(when (= part-id (wsp/structure-part-id (:row %))) %)
        (structure-locations form)))

(defn- select-part!
  [spec-id part-id]
  (let [prev (:selected @!room)]
    (when (not= prev spec-id) (select-at! spec-id))
    (swap! !room assoc :selected spec-id :part-selected [spec-id part-id])
    (when (and prev (not= prev spec-id)) (rebuild-specimen! prev))
    (rebuild-specimen! spec-id)
    (render-inspector!)))

(defn- begin-draw!
  [parent-id origin]
  (when (and parent-id (not= parent-id (:selected @!room)))
    (select-at! parent-id))
  (swap! !room assoc :gesture
         {:kind :draw :parent-id parent-id :origin origin :current origin})
  (render-draft!))

(defn- move-draw!
  [world]
  (when (= :draw (get-in @!room [:gesture :kind]))
    (swap! !room assoc-in [:gesture :current] world)
    (render-draft!)))

(defn- draw-top-frame!
  [{:keys [x y w h]}]
  (let [n (inc (:counter @!room))
        label (wsp/sketch-label n)
        style wsp/sketch-style-defaults
        pad (wsp/pad-of style)]
    (adopt-specimen!
     {:n n :id (str "wsp-" n) :kind :specimen
      :label label :name (wsp/sketch-name label)
      :x (+ x pad) :y (+ y pad)
      :text "" :caret 0 :style style
      :size-override {:w w :h h}
      :parent nil :variations 0})))

(defn- draw-inside!
  [spec-id {:keys [x y w h]}]
  (when-let [spec (get-in @!room [:specimens spec-id])]
    (let [form (canonical-form-for spec)
          n (inc (:part-counter @!room 0))
          part-id (keyword (str "frame-" n))
          local {:x (- x (:x spec)) :y (- y (:y spec)) :w w :h h}
          row (wsp/structure-part spec-id part-id local
                                  (wsp/next-part-order form))
          result (candidate-result form {:edit/op :add :part row})]
      (when (install-candidate! spec-id result)
        (swap! !room assoc :part-counter n
               :part-selected [spec-id part-id])))))

(defn- finish-draw!
  [world]
  (let [{:keys [kind parent-id origin]} (:gesture @!room)]
    (when (and (= :draw kind) origin world)
      (let [rect (wsp/drag-rect origin world)]
        (if parent-id
          (draw-inside! parent-id rect)
          (draw-top-frame! rect))))
    (swap! !room assoc :gesture nil)
    (render-draft!)))

(defn- update-structure-props!
  [spec-id part-id props]
  (when-let [spec (get-in @!room [:specimens spec-id])]
    (let [form (canonical-form-for spec)
          {:keys [where def-name index row]} (structure-location form part-id)
          result
          (case where
            :parts
            (candidate-result
             form {:edit/op :retune-props
                   :part/id (:part/id row)
                   :part/props props})

            :def
            (let [rows (get-in form [:anatomy/defs def-name])
                  rows (update rows index update :part/props merge props)]
              (candidate-result
               form {:edit/op :attach-def :def/name def-name :def/rows rows}))
            nil)]
      (install-candidate! spec-id result))))

(defn- begin-part-move!
  [spec-id part-id [wx wy]]
  (when-let [spec (get-in @!room [:specimens spec-id])]
    (when-let [row (:row (structure-location (canonical-form-for spec) part-id))]
      (let [{px :x py :y} (wsp/structure-part-rect row)
            lx (- wx (:x spec))
            ly (- wy (:y spec))]
        (select-part! spec-id part-id)
        (swap! !room assoc :gesture
               {:kind :part-move :spec-id spec-id :part-id part-id
                :grab [(- px lx) (- py ly)]})))))

(defn- move-part!
  [[wx wy]]
  (let [{:keys [kind spec-id part-id grab]} (:gesture @!room)
        spec (get-in @!room [:specimens spec-id])]
    (when (and (= :part-move kind) spec grab)
      (let [[gx gy] grab]
        (update-structure-props!
         spec-id part-id
         {:x (+ (- wx (:x spec)) gx)
          :y (+ (- wy (:y spec)) gy)})))))

(defn- bring-part-forward!
  [spec-id part-id]
  (when-let [spec (get-in @!room [:specimens spec-id])]
    (let [form (canonical-form-for spec)
          {:keys [where def-name index row]} (structure-location form part-id)
          result
          (case where
            :parts
            (candidate-result
             form {:edit/op :reorder :part/id (:part/id row)
                   :part/order (wsp/next-part-order form)})

            :def
            (let [rows (get-in form [:anatomy/defs def-name])
                  order (inc (reduce max 0 (keep :part/order rows)))
                  rows (assoc-in rows [index :part/order] order)]
              (candidate-result
               form {:edit/op :attach-def :def/name def-name :def/rows rows}))
            nil)]
      (install-candidate! spec-id result))))

(defn- edit-sequence
  [form edits]
  (reduce
   (fn [candidate edit]
     (when candidate
       (some-> (candidate-result candidate edit) :form)))
   form edits))

(defn- nest-parts!
  "Use the grammar's one legal sub-anatomy level. The target and dropped frame
   move into one attached definition; a new top-level sub-anatomy row exposes
   that group through the ordinary compiler."
  [spec-id source-id target-id]
  (when-let [spec (get-in @!room [:specimens spec-id])]
    (let [form (canonical-form-for spec)
          source (structure-location form source-id)
          target (structure-location form target-id)]
      (when (and (= :parts (:where source)) (= :parts (:where target)))
        (let [n (inc (:nest-counter @!room 0))
              def-name (keyword (str "nest-" n))
              group-id (keyword (str "group-" n))
              source-row (-> (:row source)
                             (assoc :part/order 1)
                             (assoc-in [:part/props :wsp-parent-part] target-id))
              target-row (assoc (:row target) :part/order 0)
              group-order (min (:part/order (:row source))
                               (:part/order (:row target)))
              group-row {:part/id group-id :part/prim :sub-anatomy
                         :part/when :always :part/order group-order
                         :part/props {} :part/def def-name}
              candidate
              (edit-sequence
               form
               [{:edit/op :remove :part/id (:part/id (:row source))}
                {:edit/op :remove :part/id (:part/id (:row target))}
                {:edit/op :attach-def :def/name def-name
                 :def/rows [target-row source-row]}
                {:edit/op :add :part group-row}])]
          (when candidate
            (swap! !room assoc :nest-counter n
                   :part-selected [spec-id source-id])
            (install-candidate! spec-id {:form candidate})
            true))))))

(defn- drop-target-id
  [spec-id source-id [wx wy]]
  (when-let [spec (get-in @!room [:specimens spec-id])]
    (let [form (canonical-form-for spec)
          point [(- wx (:x spec)) (- wy (:y spec))]]
      (some (fn [{:keys [row]}]
              (let [part-id (wsp/structure-part-id row)]
                (when (and (not= source-id part-id)
                           (wsp/point-in-rect?
                            point (wsp/structure-part-rect row)))
                  part-id)))
            (reverse (structure-locations form))))))

(defn- finish-part-move!
  [world]
  (let [{:keys [kind spec-id part-id]} (:gesture @!room)]
    (when (= :part-move kind)
      (move-part! world)
      (let [target-id (drop-target-id spec-id part-id world)]
        (when-not (and target-id (nest-parts! spec-id part-id target-id))
          (bring-part-forward! spec-id part-id))))
    (swap! !room assoc :gesture nil)))

;; ===========================================================================
;; Move + resize continuations (frozen facts at :begin; anchor never moves)
;; ===========================================================================

(defn- move-specimen! [id [wx wy]]
  (let [{:keys [kind grab]} (:gesture @!room)]
    (when (and (= :move kind) grab)
      (let [[gx gy] grab
            x (+ wx gx)
            y (+ wy gy)]
        (swap! !room update-in [:specimens id] assoc :x x :y y)
        (when-let [slot (ss/slot (scene-rt/store-snapshot) (spec-vi id))]
          (scene-rt/set-transform! (:container slot) {:x x :y y}))
        (render-threads-for! id)
        (when (= id (:selected @!room)) (move-overlays!))))))

(defn- natural-size
  "The smallest frame the text allows — resize can never hide words."
  [spec]
  (let [m (wsp/text-metrics (base-metrics) (:style spec))
        pad (wsp/pad-of (:style spec))
        lines (str/split (or (:text spec) "") #"\n" -1)
        max-len (reduce max 1 (map count lines))]
    [(+ (* max-len (:char-advance m)) (* 2 pad))
     (+ (* (count lines) (:line-h m)) (* 2 pad))]))

(defn- resize-specimen! [[wx wy]]
  (let [{:keys [kind id origin floor-size]} (:gesture @!room)]
    (when (and (= :resize kind) origin)
      (let [[ox oy] origin
            [mw mh] floor-size
            w (max 64.0 mw (- wx ox))
            h (max 36.0 mh (- wy oy))]
        (swap! !room assoc-in [:specimens id :size-override] {:w w :h h})
        (rebuild-specimen! id)))))

;; ===========================================================================
;; Door + camera travel
;; ===========================================================================

(defn- ease [t]
  (let [t (max 0.0 (min 1.0 t))]
    (- (* 3.0 t t) (* 2.0 t t t))))

(defn- travel-camera! [target]
  (when-let [refs @!refs]
    (let [!camera (:!camera refs)
          from @!camera
          t0 (js/performance.now)
          dur 680.0
          token (str (random-uuid))]
      (swap! !room assoc :travel token)
      (letfn [(step [_]
                (when (= token (:travel @!room))
                  (let [t (/ (- (js/performance.now) t0) dur)
                        e (ease t)
                        lerp (fn [a b] (+ a (* (- b a) e)))
                        zoom (js/Math.exp
                              (lerp (js/Math.log (max 0.0001 (:zoom from 1.0)))
                                    (js/Math.log (:zoom target))))]
                    (reset! !camera {:x (lerp (:x from) (:x target))
                                     :y (lerp (:y from) (:y target))
                                     :zoom zoom})
                    (if (< t 1.0)
                      (js/requestAnimationFrame step)
                      (swap! !room assoc :travel nil)))))]
        (js/requestAnimationFrame step)))))

(defn- place-door!
  "The one camera-following widget: world-placed FROM the live camera with
   zoom cancelled (the studio birth-handle placement pattern — pick
   inverse-transforms container+world only, so a :camera :screen container
   would render fixed yet mis-pick). Outside the room it reads `Workshop →`;
   inside it reads `← Back`."
  []
  (when-let [refs @!refs]
    (let [cam @(:!camera refs)
          zoom (max 0.0001 (:zoom cam 1.0))
          vp (:viewport (base-metrics))
          vw (or (:width vp) 1200.0)
          [wx wy] (screen->world cam (- vw 208.0) 20.0)
          in? (boolean (room-active?))
          vi [:wsp-b-panel :door]
          slot (ss/slot (scene-rt/store-snapshot) vi)]
      (if (and slot (= in? (:door-in? @!room)))
        (scene-rt/set-transform! (:container slot)
                                 {:x wx :y wy :scale (/ 1.0 zoom)})
        (do (swap! !room assoc :door-in? in?)
            (upsert! vi (if in? door-exit-tree door-enter-tree)
                     {:x wx :y wy :scale (/ 1.0 zoom) :layer 9
                      :meta {:wsp-door? true}}))))))

(defn- render-pencil! []
  (upsert! [:wsp-b-panel :pencil] (pencil-tree)
           {:x (:x wsp/pencil-pos) :y (:y wsp/pencil-pos) :layer 4
            :meta {:wsp-pencil? true}}))

(defn- render-draft! []
  (let [{:keys [kind origin current]} (:gesture @!room)]
    (if (and (= :draw kind) origin current)
      (let [{:keys [x y w h]} (wsp/drag-rect origin current)]
        (upsert! [:wsp-g-draft] (draft-tree w h)
                 {:x x :y y :layer 8 :meta {:wsp-draft? true}}))
      (close! [:wsp-g-draft]))))

(defn- ensure-room! []
  (upsert! [:wsp-a-floor] (floor-tree)
           {:x (:x wsp/floor-rect) :y (:y wsp/floor-rect) :layer 2
            :meta {:wsp-floor? true}})
  (upsert! [:wsp-b-panel :shelf] shelf-tree
           {:x (:x wsp/shelf-pos) :y (:y wsp/shelf-pos) :layer 4
            :meta {:wsp-shelf? true}})
  (render-pencil!))

(defn- render-room!
  "Re-materialize everything the room knows (idempotent — also the self-heal
   after anything upstream cleared the scene store)."
  []
  (ensure-room!)
  (doseq [id (:order @!room)] (rebuild-specimen! id))
  (render-overlays!)
  (render-draft!)
  (render-inspector!))

(defn- enter-room! []
  (when-let [refs @!refs]
    (let [vp (:viewport (base-metrics))
          vw (or (:width vp) 1200.0)
          vh (or (:height vp) 800.0)]
      (when-not (room-active?)
        (swap! !room assoc :return-camera @(:!camera refs)))
      (render-room!)
      (travel-camera! (wsp/arrival-camera vw vh)))))

(defn- exit-room! []
  (travel-camera! (or (:return-camera @!room) {:x 0.0 :y 0.0 :zoom 1.0})))

;; ===========================================================================
;; Actions — the scene-descriptor seam (tap = nil continuation)
;; ===========================================================================

(scene-rt/register-action! :wsp/enter
  (fn [_descriptor ctx]
    (when (tap? ctx)
      (clear-ground-press!)
      (enter-room!))
    true))

(scene-rt/register-action! :wsp/exit
  (fn [_descriptor ctx]
    (when (tap? ctx)
      (clear-ground-press!)
      (exit-room!))
    true))

(scene-rt/register-action! :wsp/floor
  (fn [_descriptor {:keys [continuation press world] :as ctx}]
    (case continuation
      :begin (begin-draw! nil (:press/world press))
      :move (move-draw! world)
      :end (finish-draw! world)
      (when (tap? ctx)
        (clear-ground-press!)
        (when-not (drawing?) (deselect!))))
    true))

(scene-rt/register-action! :wsp/noop
  (fn [_descriptor ctx]
    (when (tap? ctx) (clear-ground-press!))
    true))

(scene-rt/register-action! :wsp/take
  (fn [descriptor ctx]
    (when (tap? ctx)
      (clear-ground-press!)
      (take-one! (:wsp/master descriptor)))
    true))

(scene-rt/register-action! :wsp/pencil
  (fn [_descriptor ctx]
    (when (tap? ctx)
      (clear-ground-press!)
      (swap! !room update :tool #(when-not (= :draw-frame %) :draw-frame))
      (when-not (drawing?)
        (swap! !room assoc :gesture nil)
        (render-draft!))
      ;; the floor only claims threshold-drag while the pencil is armed
      (ensure-room!))
    true))

(scene-rt/register-action! :wsp/drag
  (fn [descriptor {:keys [continuation press world] :as ctx}]
    (let [id (:wsp/id descriptor)]
      (case continuation
        :begin (if (drawing?)
                 (begin-draw! id (:press/world press))
                 (when-let [spec (get-in @!room [:specimens id])]
                   ;; picking it up is already having it
                   (when (not= id (:selected @!room)) (select-at! id))
                   (let [[wx wy] (:press/world press)]
                     (swap! !room assoc :gesture
                            {:kind :move :id id
                             :grab [(- (:x spec) wx) (- (:y spec) wy)]}))))
        :move (if (= :draw (get-in @!room [:gesture :kind]))
                (move-draw! world)
                (move-specimen! id world))
        :end (if (= :draw (get-in @!room [:gesture :kind]))
               (finish-draw! world)
               (do (move-specimen! id world)
                   (swap! !room assoc :gesture nil)))
        (when (tap? ctx)
          (clear-ground-press!)
          (select-at! id))))
    true))

(scene-rt/register-action! :wsp/part-drag
  (fn [descriptor {:keys [continuation press world] :as ctx}]
    (let [spec-id (:wsp/id descriptor)
          part-id (:wsp/part-id descriptor)]
      (case continuation
        :begin (if (drawing?)
                 (begin-draw! spec-id (:press/world press))
                 (begin-part-move! spec-id part-id (:press/world press)))
        :move (if (= :draw (get-in @!room [:gesture :kind]))
                (move-draw! world)
                (move-part! world))
        :end (if (= :draw (get-in @!room [:gesture :kind]))
               (finish-draw! world)
               (finish-part-move! world))
        (when (tap? ctx)
          (clear-ground-press!)
          (select-part! spec-id part-id))))
    true))

(scene-rt/register-action! :wsp/resize
  (fn [_descriptor {:keys [continuation world] :as ctx}]
    (case continuation
      :begin (when-let [spec (selected-specimen)]
               ;; the anchor is the frame's TOP-LEFT corner, frozen HERE from
               ;; the specimen's own coordinates — never from the pointer —
               ;; and never touched again during the drag
               (let [pad (or (:pad spec) 0.0)]
                 (swap! !room assoc :gesture
                        {:kind :resize :id (:id spec)
                         :origin [(- (:x spec) pad) (- (:y spec) pad)]
                         :floor-size (natural-size spec)})))
      :move (resize-specimen! world)
      :end (do (resize-specimen! world)
               (swap! !room assoc :gesture nil))
      (when (tap? ctx) (clear-ground-press!)))
    true))

(scene-rt/register-action! :wsp/inspect
  (fn [descriptor ctx]
    (when (tap? ctx)
      (clear-ground-press!)
      (when-let [sel (selected-specimen)]
        (swap! !room update-in [:specimens (:id sel) :style]
               wsp/cycle-style (:wsp/prop descriptor))
        (rebuild-specimen! (:id sel))
        (render-inspector!)))
    true))

(scene-rt/register-action! :wsp/verb
  (fn [descriptor ctx]
    (when (tap? ctx)
      (clear-ground-press!)
      (case (:wsp/verb descriptor)
        :variation (variation!)
        :note (note!)
        nil))
    true))

;; ===========================================================================
;; Typing — ground-edit's pure keystroke grammar over the room's own bodies
;; ===========================================================================

(def ^:private room-edit-keys
  #{:char :backspace :delete :enter :left :right :up :down
    :home :end :word-left :word-right :paste})

(defn- handle-room-key! [spec ev]
  (when-let [r (ge/apply-ground-keydown
                {:text (:text spec) :caret (:caret spec)} ev)]
    (case (:op r)
      :edit (swap! !room update-in [:specimens (:id spec)] assoc
                   :text (:new-text r) :caret (:new-caret r))
      :caret (let [n (count (or (:text spec) ""))]
               (swap! !room assoc-in [:specimens (:id spec) :caret]
                      (max 0 (min (long (:new-caret r)) n))))
      nil)
    (rebuild-specimen! (:id spec))))

(defn- key-guard
  "Capture-phase keydown, live only while the camera stands in the room.
   Keys for a held specimen run the room's edit grammar; loose content keys
   are swallowed so the ordinary type-anywhere lane can never mint a durable
   note onto the room floor. Global chords and Escape pass through."
  [e]
  (when (room-active?)
    (let [event (ev/parse-key-event e)
          sel (selected-specimen)]
      (cond
        (nil? event) nil

        (:global? event)
        (when (= :escape (:type event))
          (if (drawing?)
            (do (swap! !room assoc :tool nil :gesture nil)
                (render-draft!)
                (ensure-room!))
            (deselect!)))

        (and sel (contains? room-edit-keys (:type event)))
        (do (.preventDefault e)
            (.stopImmediatePropagation e)
            (handle-room-key! sel event))

        (contains? #{:char :enter} (:type event))
        (do (.preventDefault e)
            (.stopImmediatePropagation e))

        :else nil))))

(defn- paste-guard [e]
  (when (room-active?)
    (.preventDefault e)
    (.stopImmediatePropagation e)
    (when-let [sel (selected-specimen)]
      (let [text (some-> (.-clipboardData e) (.getData "text/plain"))]
        (when (seq (or text ""))
          (handle-room-key! sel {:type :paste :text text
                                 :paste-policy nil
                                 :paste/source-mark :clipboard}))))))

(defonce ^:private !guards (atom {}))

(defn- swap-guard! [event-name handler]
  (when-let [old (get @!guards event-name)]
    (.removeEventListener js/window event-name old true))
  (swap! !guards assoc event-name handler)
  (.addEventListener js/window event-name handler true))

;; ===========================================================================
;; Install — one call from ground's install, refs in, no cycle back
;; ===========================================================================

(defn install!
  "Wire the playground: room furniture registered (it simply stands in the
   far region until visited), the camera-following door placed and kept
   placed by a camera watch, and the room's key/paste guards armed.
   Idempotent — safe on hot reload."
  [{:keys [!camera] :as refs}]
  (reset! !refs refs)
  (ensure-room!)
  (place-door!)
  (add-watch !camera ::wsp-door
             (fn [_ _ old new] (when (not= old new) (place-door!))))
  (swap-guard! "keydown" key-guard)
  (swap-guard! "paste" paste-guard)
  nil)
