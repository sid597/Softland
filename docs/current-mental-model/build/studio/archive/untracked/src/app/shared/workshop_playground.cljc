(ns app.shared.workshop-playground
  "Workshop playground cuts 1–2 — the pure half.

   DISPOSABLE PLAYGROUND (in-memory, no durability, no server writes). This
   namespace owns everything the room can decide without a browser: the
   Sid-words vocabulary over the real master registry, the appearance palettes
   the inspector cycles, the room's world geometry (a region far from the
   origin so it can never collide with real notes), connector/label math, and
   the specimen frame part that dresses a block-family instance as a table
   specimen. The cljs half (client.workspace.workshop-playground) owns atoms,
   slots, gestures, and typing.

   Law of the room: every string a human can see is a plain word. No fm:*,
   no hashes, no revision ids ever leave this map."
  (:require [app.shared.facet-masters :as facet-masters]))

;; ===========================================================================
;; Sid-words — registry ids to human words (unmapped ids stay off the shelf)
;; ===========================================================================

(def sid-words
  "Master-registry id → shelf words. `:take?` marks the one family that can
   stand on the table as a live specimen in cut 1 (a block). The rest are
   facets every block wears — named honestly, not takeable."
  {"fm:anatomy"    {:label "Block" :take? true}
   "fm:text-body"  {:label "Text"}
   "fm:attention"  {:label "Focus ring"}
   "fm:positioned" {:label "Placement"}
   "fm:foldable"   {:label "Folds"}
   "fm:provenance" {:label "Author ink"}
   "fm:threaded"   {:label "Threads"}
   "fm:space"      {:label "Camera"}
   "fm:invocation" {:label "Sending"}})

(def shelf-rows
  "The shelf's rows, read from the REAL registry order. A master with no
   clean human name renders nothing at all."
  (into []
        (keep (fn [id]
                (when-let [w (get sid-words id)]
                  {:id id
                   :label (:label w)
                   :take? (boolean (:take? w))})))
        facet-masters/master-ids))

;; ===========================================================================
;; Appearance palettes — what the inspector cycles, in plain words
;; ===========================================================================

(def fills
  [{:name "Clear" :rgba [0.0 0.0 0.0 0.0]}
   {:name "Slate" :rgba [0.16 0.19 0.26 0.35]}
   {:name "Moss"  :rgba [0.13 0.23 0.17 0.42]}
   {:name "Wine"  :rgba [0.26 0.14 0.18 0.42]}
   {:name "Sand"  :rgba [0.28 0.24 0.13 0.36]}])

(def frames
  [{:name "Steel" :rgba [0.50 0.56 0.68 0.95]}
   {:name "Amber" :rgba [0.92 0.72 0.30 0.95]}
   {:name "Sky"   :rgba [0.40 0.62 0.88 0.95]}
   {:name "Moss"  :rgba [0.42 0.72 0.50 0.95]}
   {:name "Rose"  :rgba [0.88 0.46 0.56 0.95]}])

(def corners [0.0 4.0 8.0 16.0])
(def text-sizes [12.0 14.0 16.0 20.0])
(def paddings [6.0 10.0 16.0 24.0])

(def specimen-style-defaults
  {:fill 1 :frame 0 :corner 2 :text-size 2 :pad 2})

(def note-style-defaults
  "Notes read as margin ink: smaller, tighter, dimmer than the thing they
   annotate."
  {:fill 1 :frame 0 :corner 1 :text-size 0 :pad 1})

(def sketch-style-defaults
  "A structure-pencil frame starts as outline, not filled content."
  (assoc specimen-style-defaults :fill 0))

(defn fill-of [style] (nth fills (mod (:fill style 0) (count fills))))
(defn frame-of [style] (nth frames (mod (:frame style 0) (count frames))))
(defn corner-of [style] (nth corners (mod (:corner style 0) (count corners))))
(defn text-size-of [style]
  (nth text-sizes (mod (:text-size style 0) (count text-sizes))))
(defn pad-of [style] (nth paddings (mod (:pad style 0) (count paddings))))

(defn cycle-style
  "One inspector click: advance one property's index by one, wrapping."
  [style prop]
  (let [n (case prop
            :fill (count fills)
            :frame (count frames)
            :corner (count corners)
            :text-size (count text-sizes)
            :pad (count paddings)
            1)]
    (update style prop (fn [i] (mod (inc (or i 0)) n)))))

(defn inherited-style
  "Materialize a fork's own complete style map. In particular, text size is
   copied from the worn parent instead of falling back through fresh defaults."
  [style]
  (merge specimen-style-defaults (or style {})))

(defn inspector-rows
  "The 4-6 plain-word rows the inspector shows for one specimen. `:swatch`
   carries a color chip for the color rows; number rows name their value."
  [style]
  [{:prop :fill :text (str "Fill · " (:name (fill-of style)))
    :swatch (assoc (vec (:rgba (fill-of style))) 3 1.0)}
   {:prop :frame :text (str "Frame · " (:name (frame-of style)))
    :swatch (:rgba (frame-of style))}
   {:prop :corner :text (str "Corner · " (long (corner-of style)) " px")}
   {:prop :text-size :text (str "Text size · " (long (text-size-of style)) " px")}
   {:prop :pad :text (str "Padding · " (long (pad-of style)) " px")}])

;; ===========================================================================
;; Room geometry — a region of the SAME world, far from every real note
;; ===========================================================================

(def room-center [100000.0 0.0])

(def floor-rect
  "The room's backdrop panel. Everything in the room stands on this."
  {:x 98800.0 :y -750.0 :w 2400.0 :h 1500.0})

(def room-margin
  "How far past the floor the room still counts as `in the room` (the typing
   guard and the door's direction both key off this)."
  500.0)

(def arrival-zoom 0.72)

(def shelf-pos {:x 98860.0 :y -690.0})
(def shelf-w 360.0)

(def inspector-w 380.0)
(def inspector-pos
  {:x (- (+ (:x floor-rect) (:w floor-rect)) inspector-w 60.0)
   :y -690.0})

(def table-origin {:x 99420.0 :y -420.0})

(def pencil-pos {:x 99420.0 :y -690.0})

(defn in-room?
  "Is a world point inside the room (floor + margin)?"
  [wx wy]
  (let [{:keys [x y w h]} floor-rect
        m room-margin]
    (and (>= wx (- x m)) (<= wx (+ x w m))
         (>= wy (- y m)) (<= wy (+ y h m)))))

(defn arrival-camera
  "The camera that frames the room: room center at viewport center.
   screen = world·zoom + pan, so pan = screen-center − world-center·zoom."
  [vw vh]
  (let [[cx cy] room-center
        z arrival-zoom]
    {:x (- (/ vw 2.0) (* cx z))
     :y (- (/ vh 2.0) (* cy z))
     :zoom z}))

(defn next-specimen-position
  "Where the nth take lands on the table: a diagonal cascade that wraps into
   a fresh column every eight specimens."
  [n]
  {:x (+ (:x table-origin) (* 56.0 (mod n 8)) (* 340.0 (quot n 8)))
   :y (+ (:y table-origin) (* 44.0 (mod n 8)))})

(defn drag-rect
  "Normalize two world points into an anchored rectangle. A gesture that only
   just crossed the pointer threshold still produces a graspable empty frame."
  [[ax ay] [bx by]]
  (let [x (min ax bx)
        y (min ay by)
        w (Math/abs (- bx ax))
        h (Math/abs (- by ay))]
    {:x x :y y :w (max 64.0 w) :h (max 44.0 h)}))

(defn point-in-rect?
  [[px py] {:keys [x y w h]}]
  (and (<= x px (+ x w))
       (<= y py (+ y h))))

(defn note-position
  "Cascade margin notes beside one parent. Eight notes make a column; the next
   eight begin a new column so annotation never degenerates into one pile."
  [parent note-index]
  {:x (+ (:x parent) (or (:w parent) 240.0) 120.0
         (* 252.0 (quot note-index 8)))
   :y (+ (- (:y parent) 8.0) (* 78.0 (mod note-index 8)))})

;; ===========================================================================
;; Labels — the subject is always named, in plain words
;; ===========================================================================

(defn specimen-label [n] (str "specimen " n))

(defn variation-label
  "First variation of `specimen 1` is `specimen 1.2` (the original holds the
   .1 seat implicitly); the next is `specimen 1.3`."
  [parent-label existing-variations]
  (str parent-label "." (+ 2 existing-variations)))

(defn specimen-name [label] (str "Block — " label))
(defn note-name [parent-label] (str "Note — beside " parent-label))
(defn sketch-label [n] (str "sketch " n))
(defn sketch-name [label] (str "Frame — " label))

;; ===========================================================================
;; Connector math — the visible thread between two bodies
;; ===========================================================================

(defn connector-segments
  "Two thin axis-aligned segments joining a parent frame to a child frame:
   horizontal from the parent's right-center toward the child's center
   column, then vertical up/down into the child. Both rects are WORLD
   coords; each renders as its own thin material box."
  [{fx :x fy :y fw :w fh :h} {tx :x ty :y tw :w th :h}]
  (let [fcy (+ fy (/ fh 2.0))
        tcx (+ tx (/ tw 2.0))
        tcy (+ ty (/ th 2.0))
        right (+ fx fw)
        x0 (min right tcx)
        x1 (max right tcx)
        y0 (min fcy tcy)
        y1 (max fcy tcy)]
    [{:x x0 :y (- fcy 1.0) :w (max 2.0 (- x1 x0)) :h 2.0}
     {:x (- tcx 1.0) :y y0 :w 2.0 :h (max 2.0 (- y1 y0))}]))

;; ===========================================================================
;; Specimen frame — the one part the room adds to a real block's anatomy
;; ===========================================================================

(defn frame-part
  "The specimen's always-visible frame, expressed as an ordinary anatomy part
   over the room's own `:wsp-frame` primitive (registered only in the room's
   registry value — the shared vocabulary is untouched). Sits between the
   selection wash (10) and the attention box (30) so focus still reads."
  [style]
  {:part/id :wsp-frame
   :part/prim :wsp-frame
   :part/when :always
   :part/order 25
   :part/props
   {:w [:view :block-w]
    :h [:view :block-h]
    :pad (pad-of style)
    :radius (corner-of style)
    :border-width 1.5
    :border-color (:rgba (frame-of style))
    :bg (:rgba (fill-of style))}})

(defn structure-part
  "One frame drawn inside a specimen, expressed as a legal ordinary `:box`
   anatomy row. The room-local box primitive interprets x/y and the private
   pick metadata; the material grammar still validates and composes the row."
  [spec-id part-id {:keys [x y w h]} order]
  {:part/id part-id
   :part/prim :box
   :part/when :always
   :part/order order
   :part/props
   {:x x :y y :w w :h h
    :padding 0.0 :gap 0.0 :radius 5.0
    :border-width 1.5
    :border-color [0.62 0.68 0.80 0.96]
    :bg [0.10 0.13 0.18 0.18]
    :wsp-structure? true
    :wsp-spec-id spec-id
    :wsp-part-id part-id}})

(defn structure-part?
  [row]
  (true? (get-in row [:part/props :wsp-structure?])))

(defn structure-part-id
  [row]
  (get-in row [:part/props :wsp-part-id]))

(defn structure-part-rect
  [row]
  (when (structure-part? row)
    (select-keys (:part/props row) [:x :y :w :h])))

(defn next-part-order
  "A fresh integer order above every current top-level row."
  [form]
  (inc (reduce max 0 (keep :part/order (:anatomy/parts form)))))

(defn text-metrics
  "Per-specimen text metrics from the base metrics' font ratio: the inspector
   cycles the size; advance and line height follow the land's own ratio."
  [{:keys [font-size char-advance]} style]
  (let [base-fs (if (and (number? font-size) (pos? font-size)) font-size 19.0)
        ratio (if (and (number? char-advance) (pos? char-advance))
                (/ char-advance base-fs)
                0.56)
        fs (text-size-of style)]
    {:font-size fs
     :char-advance (* fs ratio)
     :line-h (Math/round (* fs 1.4))}))
