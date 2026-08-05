(ns app.client.workspace.rect-tree
  "Scene graph for nested UI.
   Everything is a rect. The tree replaces scattered compute-*-rects fns with
   one generic walk that produces flat GPU-compatible vectors."
  (:require [app.client.substrate.image-material :as image-material]
            [app.client.workspace.text-layout :as tl]))

(defn wrap-line
  "Compatibility name for Contract-T's legacy word wrapper."
  [line max-chars]
  (tl/wrap-line line max-chars))

(defn rt-node
  "Create a rect tree node.  Bounds are in parent-relative coordinates.
   Children are rendered back-to-front (painter's order).
   Optional :layout {:direction :column/:row :gap N :padding N :align :start/:center/:end :auto-height? bool}
   enables automatic child positioning via resolve-layout."
  [id type bounds & {:keys [style actions children text clip? data layout]
                     :or {clip? false}}]
  {:id       id
   :type     type
   :bounds   bounds
   :style    (or style {})
   :actions  (or actions {})
   :children (vec (or children []))
   :text     (or text [])
   :clip?    clip?
   :data     data
   :layout   layout})

;; --- Layout engine ----------------------------------------------------------
;; Pure pre-pass: walks tree depth-first, computes child :x/:y from :layout
;; directives. Nodes without :layout pass through unchanged.

(defn normalize-padding
  "CSS-style padding shorthand:
   number        -> [n n n n]       (uniform)
   [vert horiz]  -> [v h v h]       (vertical, horizontal)
   [t r b l]     -> [t r b l]       (clockwise from top)"
  [p]
  (cond
    (number? p)               [p p p p]
    (nil? p)                  [0 0 0 0]
    (and (vector? p) (= 2 (count p))) [(nth p 0) (nth p 1) (nth p 0) (nth p 1)]
    (and (vector? p) (= 4 (count p))) p
    :else                     [0 0 0 0]))

(defn layout-children
  "Position children inside a parent node according to its :layout directive.
   Returns the node with children's :bounds :x/:y updated.
   Children with (:data child :layout-skip?) pass through unchanged.

   Layout keys:
     :direction   :column (default) or :row
     :gap         px between children (default 0)
     :padding     number, [v h], or [t r b l] (default 0)
     :align       :start (default), :center, or :end — cross-axis alignment
     :auto-height? if true, parent :h = content height + padding"
  [node]
  (let [layout   (:layout node)
        bounds   (:bounds node)
        parent-w (:w bounds 0)
        parent-h (:h bounds 0)]
    (if-not layout
      node ;; no layout directive -> pass through
      (let [{:keys [direction gap padding align auto-height?]
             :or   {direction :column gap 0 align :start}} layout
            [pt pr pb pl] (normalize-padding padding)
            children (:children node)]
        (if (empty? children)
          node
          (let [;; Separate layout-managed children from skip children
                positioned
                (loop [cs       children
                       cursor   (if (= direction :column) pt pl) ;; start after top/left padding
                       result   []]
                  (if (empty? cs)
                    result
                    (let [child (first cs)]
                      (if (get-in child [:data :layout-skip?])
                        ;; Skip — preserve as-is
                        (recur (rest cs) cursor (conj result child))
                        ;; Position this child
                        (let [cb    (:bounds child)
                              cw    (:w cb 0)
                              ch    (:h cb 0)
                              ;; Cross-axis position
                              cross (case direction
                                      :column
                                      (case align
                                        :center (+ pl (/ (- parent-w pl pr cw) 2))
                                        :end    (- parent-w pr cw)
                                        ;; :start
                                        pl)
                                      :row
                                      (case align
                                        :center (+ pt (/ (- parent-h pt pb ch) 2))
                                        :end    (- parent-h pb ch)
                                        ;; :start
                                        pt))
                              ;; Set x/y based on direction
                              new-bounds (if (= direction :column)
                                           (assoc cb :x cross :y cursor)
                                           (assoc cb :y cross :x cursor))
                              new-child  (assoc child :bounds new-bounds)
                              ;; Advance cursor along main axis
                              advance    (if (= direction :column) ch cw)
                              next-cursor (+ cursor advance gap)]
                          (recur (rest cs) next-cursor (conj result new-child)))))))
                ;; Auto-height: shrink-wrap parent to content
                total-main (if auto-height?
                             (let [managed (filterv #(not (get-in % [:data :layout-skip?])) positioned)
                                   last-child (peek managed)]
                               (when last-child
                                 (let [lb (:bounds last-child)]
                                   (+ (if (= direction :column)
                                        (+ (:y lb 0) (:h lb 0) pb)
                                        (+ (:x lb 0) (:w lb 0) pr))))))
                             nil)
                new-bounds (if total-main
                             (if (= direction :column)
                               (assoc bounds :h total-main)
                               (assoc bounds :w total-main))
                             bounds)]
            (assoc node :children positioned :bounds new-bounds)))))))

(defn resolve-text-layout
  "Auto-position text ops on a node that has :text-layout.
   Text-layout map: {:line-height N :max-chars N :padding [t r b l] or N}
   Text ops provide :text, :size, :r/:g/:b/:a, :type — but NOT :x/:y.
   This fn computes :x/:y by wrapping text and stacking lines vertically.
   Returns the node with :text updated (local coords)."
  [node]
  (let [tl (:text-layout node)]
    (if-not tl
      node
      (let [{:keys [line-height max-chars padding provider]} tl
            [pt _pr _pb pl] (normalize-padding padding)
            text-specs (:text node)]
        (if (empty? text-specs)
          node
          (let [ops (loop [specs text-specs
                           y     pt
                           acc   []]
                     (if (empty? specs)
                       acc
                       (let [spec  (first specs)
                             txt   (:text spec "")
                             size  (:size spec 14)
                             lh    (or line-height size)
                             layout-result
                             (tl/layout {:text txt
                                         :provider provider
                                         :font-size size
                                         :char-advance (tl/legacy-char-advance size 0.56)
                                         :line-height lh
                                         :origin [pl y]
                                         :baseline-offset 0
                                         :max-chars max-chars
                                         :wrap-policy :word})
                             lines (:lines (tl/wrap-result layout-result))
                             line-ops (tl/line-paint-ops layout-result spec)
                             next-y (+ y (get-in (tl/measure-result layout-result)
                                                 [:metrics :stack-advance]))]
                         (recur (rest specs) next-y (into acc line-ops)))))]
            (assoc node :text ops)))))))

(defn resolve-layout
  "Recursive depth-first pre-pass: apply layout-children at each level,
   resolve text-layout, then recurse into children. Returns a fully-positioned
   tree ready for tree->rects / tree->text-ops / tree->shadows."
  [node]
  (let [laid-out  (-> node layout-children resolve-text-layout)
        children  (:children laid-out)]
    (if (empty? children)
      laid-out
      (assoc laid-out :children (mapv resolve-layout children)))))

;; --- Tree walk: rects -------------------------------------------------------

(defn- intersect-clip
  "Axis-aligned intersection of a :clip? node's own absolute bounds with the
   clip already in force from its ancestors. A child may never paint outside
   EITHER — replacing the ancestor clip (the pre-2026-07-05 behavior) let a
   :clip? node's children escape the viewport clip, which forced trail-room R-1
   to drop the expansion's clip? entirely (and with it card-width text
   truncation). nil ancestor clip (every production walk today) → the node's
   own bounds, byte-identical to the old behavior. Degenerate (empty)
   intersections are fine: visible? checks downstream go false."
  [abs-x abs-y w h clip-bounds]
  (if-not clip-bounds
    {:x abs-x :y abs-y :w w :h h}
    (let [x1 (max abs-x (:x clip-bounds))
          y1 (max abs-y (:y clip-bounds))
          x2 (min (+ abs-x w) (+ (:x clip-bounds) (:w clip-bounds)))
          y2 (min (+ abs-y h) (+ (:y clip-bounds) (:h clip-bounds)))]
      {:x x1 :y y1 :w (- x2 x1) :h (- y2 y1)})))

(defn tree->rects
  "Walk rect tree depth-first, emit flat vector of GPU rect maps.
   Parent-relative coords are converted to absolute via parent-x/parent-y.
   Clip-bounds is {:x :y :w :h} in absolute space (nil = no clipping).
   Style keys: :bg, :radius, :corner-radii, :border-width, :border-widths,
               :border-color, :gradient, :gradient-color2"
  ([node] (tree->rects node 0 0 nil))
  ([node parent-x parent-y clip-bounds]
   (let [{:keys [bounds style children clip?]} node
         abs-x (+ parent-x (:x bounds 0))
         abs-y (+ parent-y (:y bounds 0))
         w     (:w bounds 0)
         h     (:h bounds 0)
         ;; If parent clips, check visibility
         visible? (if clip-bounds
                    (let [cx (:x clip-bounds) cy (:y clip-bounds)
                          cw (:w clip-bounds) ch (:h clip-bounds)]
                      (and (< abs-x (+ cx cw))
                           (< abs-y (+ cy ch))
                           (> (+ abs-x w) cx)
                           (> (+ abs-y h) cy)))
                    true)]
     (when visible?
       (let [;; T-4 clamp: partially-visible bg rects are clamped to the
             ;; axis-aligned intersection with clip bounds. visible? above
             ;; guarantees the intersection is non-empty. Radii degrade at
             ;; clamped corners (accepted, view-mvp contract §5.2).
             bx (if clip-bounds (max abs-x (:x clip-bounds)) abs-x)
             by (if clip-bounds (max abs-y (:y clip-bounds)) abs-y)
             bw (if clip-bounds
                  (- (min (+ abs-x w) (+ (:x clip-bounds) (:w clip-bounds))) bx)
                  w)
             bh (if clip-bounds
                  (- (min (+ abs-y h) (+ (:y clip-bounds) (:h clip-bounds))) by)
                  h)
             ;; Background rect from style — now includes SDF properties
             bg  (when-let [c (:bg style)]
                   (cond-> {:x bx :y by :w bw :h bh
                            :r (nth c 0) :g (nth c 1) :b (nth c 2) :a (nth c 3)}
                     ;; Carry node identity for keyed differential rendering (Phase 5)
                     (:id node)              (assoc :id (:id node))
                     (:radius style)         (assoc :radius (:radius style))
                     (:corner-radii style)   (assoc :corner-radii (:corner-radii style))
                     (:border-width style)   (assoc :border-width (:border-width style))
                     (:border-widths style)  (assoc :border-widths (:border-widths style))
                     (:border-color style)   (assoc :border-color (:border-color style))
                     (:gradient style)       (assoc :gradient (:gradient style))
                     (:gradient-color2 style)(assoc :gradient-color2 (:gradient-color2 style))))
             ;; This node's clip bounds for children (if clip? is set):
             ;; INTERSECTED with the ancestor clip, never replacing it
             child-clip (if clip?
                          (intersect-clip abs-x abs-y w h clip-bounds)
                          clip-bounds)
             ;; Recurse children (depth-first, painter's order)
             child-rects (into [] (mapcat #(tree->rects % abs-x abs-y child-clip)) children)]
         (cond-> []
           bg   (conj bg)
           true (into child-rects)))))))

;; --- Tree walk: image ops ---------------------------------------------------

(defn tree->images
  "Walk image-bearing rt-nodes depth-first and emit ordered image ops.

   Presence is `[:data :image/digest]`.  Every such node must carry its own
   semantic `[:data :address]`; falling back to an addressed ancestor would
   violate the image pick owner route.  Clip clamps shrink placement and inset
   crop coordinates by the same fractions, so a partial clip crops rather
   than stretches (IMAGE-ATOM T15)."
  ([node] (tree->images node 0 0 nil))
  ([node parent-x parent-y clip-bounds]
   (let [{:keys [bounds children clip? data]} node
         abs-x (+ parent-x (:x bounds 0))
         abs-y (+ parent-y (:y bounds 0))
         width (:w bounds 0)
         height (:h bounds 0)
         digest (:image/digest data)
         visible? (if clip-bounds
                    (let [clip-x (:x clip-bounds)
                          clip-y (:y clip-bounds)
                          clip-width (:w clip-bounds)
                          clip-height (:h clip-bounds)]
                      (and (< abs-x (+ clip-x clip-width))
                           (< abs-y (+ clip-y clip-height))
                           (> (+ abs-x width) clip-x)
                           (> (+ abs-y height) clip-y)))
                    true)]
     (when (and digest (nil? (:address data)))
       (throw (ex-info "Image rt-node requires its own semantic address"
                       {:node/id (:id node) :image/digest digest})))
     (when visible?
       (let [intrinsic (or (:image/intrinsic-size data)
                           [(:image/width data) (:image/height data)])
             crop (when digest
                    (image-material/normalize-crop intrinsic
                                                   (:image/crop data)))
             clipped (when digest
                       (image-material/clip-placement
                        {:x abs-x :y abs-y :w width :h height}
                        crop clip-bounds))
             own-op
             (when clipped
               (let [{placed :placement clipped-crop :crop} clipped]
                 ;; IMAGE-ATOM T8/T14: the op carries only the existing compact
                 ;; container index when stamped; never a per-node matrix.
                 (merge placed
                        {:id (:id node)
                         :address (:address data)
                         :image/digest digest
                         :image/color-tag (:image/color-tag data)
                         :image/alpha-association
                         (or (:image/alpha-association data) :straight)
                         :image/intrinsic-size intrinsic
                         :image/crop clipped-crop
                         :image/uv (image-material/crop->uv intrinsic clipped-crop)
                         :image/opacity (or (:image/opacity data) 1.0)
                         :image/tint (or (:image/tint data)
                                         [1.0 1.0 1.0 1.0])})))
             child-clip (if clip?
                          (intersect-clip abs-x abs-y width height clip-bounds)
                          clip-bounds)
             child-ops (into []
                             (mapcat #(tree->images % abs-x abs-y child-clip))
                             children)]
         (cond-> []
           own-op (conj own-op)
           true (into child-ops)))))))

;; --- Tree walk: text ops ----------------------------------------------------

(defn tree->text-ops
  "Walk rect tree depth-first, emit nested vector of text-op vectors.
   Text ops on each node have :x/:y in node-local space; the walk
   offsets them to absolute coordinates.  Returns [[{op}] ...]."
  ([node] (tree->text-ops node 0 0 nil))
  ([node parent-x parent-y clip-bounds]
   (let [{:keys [bounds style children text clip?]} node
         abs-x (+ parent-x (:x bounds 0))
         abs-y (+ parent-y (:y bounds 0))
         w     (:w bounds 0)
         h     (:h bounds 0)
         visible? (if clip-bounds
                    (let [cx (:x clip-bounds) cy (:y clip-bounds)
                          cw (:w clip-bounds) ch (:h clip-bounds)]
                      (and (< abs-x (+ cx cw))
                           (< abs-y (+ cy ch))
                           (> (+ abs-x w) cx)
                           (> (+ abs-y h) cy)))
                    true)]
     (when visible?
       (let [;; Clip bounds: one Contract-T clip plan for horizontal substring
             ;; and vertical baseline visibility.
             clip-right (when clip-bounds (+ (:x clip-bounds) (:w clip-bounds)))
             clip-top (when clip-bounds (:y clip-bounds))
             clip-bottom (when clip-bounds (+ (:y clip-bounds) (:h clip-bounds)))
             clip-op (fn [op]
                       (let [fs (:size op 14)
                             existing (:layout-result op)
                             layout-result
                             (if existing
                               (let [clip {:right (some-> clip-right (- abs-x))
                                           :top (some-> clip-top (- abs-y))
                                           :bottom (some-> clip-bottom (- abs-y))}]
                                 (-> existing
                                     (assoc-in [:constraints :clip] clip)
                                     (assoc-in [:clip-plan :clip-geometry] clip)))
                               (tl/layout {:text (:text op "")
                                           :source-lines [(:text op "")]
                                           :font-size fs
                                           :char-advance (tl/legacy-char-advance fs 0.56)
                                           :line-height fs
                                           :origin [(:x op 0) (:y op 0)]
                                           :clip {:right (some-> clip-right (- abs-x))
                                                  :top (some-> clip-top (- abs-y))
                                                  :bottom (some-> clip-bottom (- abs-y))}}))]
                         (:op (tl/clip-result layout-result op
                                             :range-mode :right-only))))
             ;; Offset this node's text ops to absolute space + clip truncation
             own-ops (when (seq text)
                       (mapv (fn [op]
                               (if (vector? op)
                                 ;; op is already a vec of text-op maps (nested format)
                                 (into [] (keep (fn [sub]
                                                  (some-> (clip-op sub)
                                                          (update :x + abs-x)
                                                          (update :y + abs-y)))
                                                op))
                                 ;; Single text-op map
                                 (when-let [clipped (clip-op op)]
                                   [(-> clipped
                                        (update :x + abs-x)
                                        (update :y + abs-y))])))
                             text))
             child-clip (if clip?
                          (intersect-clip abs-x abs-y w h clip-bounds)
                          clip-bounds)
             child-ops (into [] (mapcat #(tree->text-ops % abs-x abs-y child-clip)) children)]
         (into (vec (filterv some? (or own-ops []))) child-ops))))))

;; --- Tree walk: shadows -----------------------------------------------------

(defn tree->shadows
  "Walk rect tree depth-first, emit flat vector of shadow maps.
   Only nodes with :shadow in style produce shadows.
   Shadow map keys: :x :y :w :h :blur :offset-x :offset-y :spread :color :radius :corner-radii"
  ([node] (tree->shadows node 0 0))
  ([node parent-x parent-y]
   (let [{:keys [bounds style children]} node
         abs-x (+ parent-x (:x bounds 0))
         abs-y (+ parent-y (:y bounds 0))
         w     (:w bounds 0)
         h     (:h bounds 0)
         shadow-spec (:shadow style)
         own-shadow (when shadow-spec
                      (let [s shadow-spec]
                        {:x abs-x :y abs-y :w w :h h
                         :blur     (or (:blur s) 8.0)
                         :offset-x (or (:offset-x s) 0.0)
                         :offset-y (or (:offset-y s) 0.0)
                         :spread   (or (:spread s) 0.0)
                         :color    (or (:color s) [0 0 0 0.25])
                         :radius   (:radius style)
                         :corner-radii (:corner-radii style)}))
         child-shadows (into [] (mapcat #(tree->shadows % abs-x abs-y)) children)]
     (cond-> []
       own-shadow (conj own-shadow)
       true       (into child-shadows)))))

;; --- Hit testing ------------------------------------------------------------

(defn hit-test
  "Find the deepest node containing point (px, py).
   Returns a vector of nodes from root to deepest hit [root ... leaf],
   or nil if the point misses the tree entirely.
   The LAST element is the deepest (innermost) hit — the event target.
   Earlier elements are ancestors — used for bubbling."
  ([node px py] (hit-test node px py 0 0))
  ([node px py parent-x parent-y]
   (let [{:keys [bounds children]} node
         abs-x (+ parent-x (:x bounds 0))
         abs-y (+ parent-y (:y bounds 0))
         w     (:w bounds 0)
         h     (:h bounds 0)]
     (when (and (>= px abs-x) (< px (+ abs-x w))
                (>= py abs-y) (< py (+ abs-y h)))
       ;; Point is inside this node — check children (reverse order = front-to-back)
       (let [child-hit (some (fn [child]
                               (hit-test child px py abs-x abs-y))
                             (rseq children))]
         (if child-hit
           (into [node] child-hit)
           [node]))))))

;; --- Event dispatch with bubbling -------------------------------------------

(defn dispatch-event
  "Dispatch an event to the hit-test path (innermost -> outermost).
   event-type is a keyword (:click, :scroll, etc.).
   event is the event data map.
   path is the hit-test result [root ... target].
   Walks from target to root (bubbling).  First handler that returns
   a non-nil value stops propagation.  Returns {:handled? bool :result any}."
  [path event-type event]
  (when (seq path)
    (loop [nodes (rseq path)]  ;; target first, root last
      (if-let [node (first nodes)]
        (let [handler (get-in node [:actions event-type])]
          (if (and handler (fn? handler))
            (let [result (handler node event)]
              (if (some? result)
                {:handled? true :result result :node node}
                (recur (rest nodes))))  ;; nil = let it bubble
            (recur (rest nodes))))
        {:handled? false}))))
