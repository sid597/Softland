(ns app.client.view.core
  "The first client: one view run on the executor and shown by the CPU runner.
   Takes the browser: a canvas, the pointer and the keyboard. Gives the
   view's painting on the canvas, a readout of what the pointer is on, and
   the view's records to edit in place. Holds one store atom, one view id and
   the last frame. The presenter here is the CPU walker's own display through
   a 2D canvas; the GPU compositor's quad is the next runner and is not bound.
   Evidence: probes/text-tool-on-the-waist/view (the driven checkpoints)."
  (:require [app.client.engine.surface :as surface]
            [app.client.engine.color :as color]
            [app.client.engine.executor :as executor]
            [app.client.path.source :as source]
            [app.client.path.surface :as path-surface]
            [app.client.text.truetype :as truetype]
            [app.client.view.records :as records]
            [app.client.view.store :as store]
            [app.client.view.run :as run]
            [app.client.view.table :as table]
            [cljs.reader :as reader]
            [cljs.pprint :as pprint]
            [clojure.string :as str]))

(defonce !store (atom records/store))
(defonce !view-id (atom "view-1"))
(defonce !frames (atom {}))
(def !frame
  "The current view's last frame: read through the frames kept per view,
   so switching views and back resumes where that view stood."
  (reify IDeref (-deref [_] (get @!frames @!view-id))))
(defonce !metrics (atom {}))
(defonce !editing (atom nil))
(defonce !table (atom table/table))

(declare frame! render-readout!)

(defn- el [id] (js/document.getElementById id))
(defn- view [] (store/record @!store @!view-id))
(defn- cursor [] (store/record @!store (get-in (view) [:pins :cursor])))

(defn- selection
  "The cursor → [from to) of its selection, or nil."
  [{:keys [anchor offset]}]
  (when (and anchor (not= anchor offset)) [(min anchor offset) (max anchor offset)]))

(defn- selected-text []
  (let [c (cursor)]
    (when-let [[from to] (selection c)]
      (apply str (subvec (table/fold-keys (:keys (store/record @!store (:in c)))) from to)))))

(defonce !dragging (atom false))

(defn- offset-at
  "A hit and the local point → the text offset the pointer stands at: before
   the glyph on the left half of its advance, after it on the right."
  [hit [px _]]
  (let [[x _] (:where hit)]
    (if (> px (+ x (/ (:advance hit) 2))) (inc (:index hit)) (:index hit))))

(defn- move-cursor! [offset anchor]
  (let [c (cursor)]
    (swap! !store store/edit (:id c) assoc :offset offset :anchor anchor)
    (frame!)))

(defn- line-height [] (get-in (store/record @!store (:hit-tool (view))) [:tool :height] 14))

(defn- move-line!
  "The caret one line up or down: the hit at the caret's x on that line,
   and the offset there; nothing when no glyph stands on that line."
  [direction]
  (when-let [[x y] (:caret @!frame)]
    (let [f @!frame probe [(+ x 0.01) (+ y (* direction (line-height)) 1)]
          hit (run/hit @!store (view) f probe {:table @!table})]
      (when (and hit (= (:run hit) (:in (cursor))))
        (move-cursor! (offset-at hit probe) nil)))))

(def srgb-lut
  "Linear [0,1] in 4096 steps → the sRGB 8-bit channel, the transfer curve
   evaluated once instead of per texel per frame."
  (let [lut (js/Uint8ClampedArray. 4096)]
    (dotimes [i 4096]
      (aset lut i (js/Math.round (* 255 (color/linear->srgb-channel (/ i 4095))))))
    lut))

(defn present!
  "The runs' surfaces at their places → the canvas: each premultiplied
   linear RGBA32F surface composed source-over at its box's texel offset,
   the whole composed over white and encoded as straight sRGB 8-bit. The
   CPU runner's own display; the GPU compositor's quads are the next runner."
  [canvas view paintings]
  (let [width (.-width canvas) height (.-height canvas)
        acc (js/Float32Array. (* width height 4))
        ctx (.getContext canvas "2d")
        img (.createImageData ctx width height)
        px (.-data img)]
    (doseq [{:keys [box surface]} paintings]
      (let [[tx ty] (run/local->texel view [(:x box) (:y box)])
            ox (js/Math.round tx) oy (js/Math.round ty)
            sw (:width surface) sh (:height surface) data (:data surface)]
        (dotimes [y sh]
          (let [dy (+ oy y)]
            (when (and (<= 0 dy) (< dy height))
              (dotimes [x sw]
                (let [dx (+ ox x)]
                  (when (and (<= 0 dx) (< dx width))
                    (let [s (* 4 (+ x (* y sw))) d (* 4 (+ dx (* dy width)))
                          keep (- 1.0 (aget data (+ s 3)))]
                      (dotimes [c 4]
                        (aset acc (+ d c) (+ (aget data (+ s c)) (* keep (aget acc (+ d c)))))))))))))))
    (dotimes [i (* width height)]
      (let [j (* 4 i) k (- 1.0 (aget acc (+ j 3)))]
        (dotimes [c 3]
          (aset px (+ j c) (aget srgb-lut (js/Math.round (* 4095 (min 1.0 (+ (aget acc (+ j c)) k)))))))
        (aset px (+ j 3) 255)))
    (.putImageData ctx img 0 0)))

(defn- pretty [x] (with-out-str (pprint/pprint x)))

(defn- render-readout! []
  (let [{:keys [frame-ms present-ms pointer statuses keys-ms tool-ms resumed]} @!metrics
        v (view)]
    (set! (.-textContent (el "readout"))
          (str "view " (:id v) " by " (:by v) " zoom " (:zoom v) " subject " (pr-str (:subject v))
               (when (:from v) (str " from " (:from v))) " cursor " (get-in v [:pins :cursor]) "\n"
               "frame " (when frame-ms (.toFixed frame-ms 1)) " ms · present " (when present-ms (.toFixed present-ms 1)) " ms"
               (when keys-ms (str " · last key " (.toFixed keys-ms 1) " ms")) "\n"
               "tools " (pr-str statuses) "\n"
               "tool ms " (pr-str tool-ms) " · resumed " (pr-str resumed) "\n"
               "cursor " (pr-str (select-keys (cursor) [:offset :anchor])) (when-let [t (selected-text)] (str " selected " (pr-str t)))
               (when-let [copied (:copied @!metrics)] (str " · copied " (pr-str copied))) "\n"
               "pointer " (pr-str (:local pointer)) " → " (pretty (:hit pointer))))))

(defn- render-views!
  "The store's view records as the selector's options; the current one selected."
  []
  (let [select (el "views") ids (map :id (filter #(= :view (:kind %)) (store/records @!store)))]
    (when-not (= (vec ids) (vec (map #(.-value %) (array-seq (.-options select)))))
      (set! (.-innerHTML select) "")
      (doseq [id ids]
        (let [o (js/document.createElement "option")]
          (set! (.-value o) id) (set! (.-textContent o) id)
          (.appendChild select o))))
    (set! (.-value select) @!view-id)))

(defn- render-records! []
  (let [v (view) f @!frame
        ids (distinct (concat [(:id v)] (:tools v) [(:hit-tool v)] (vals (:pins v)) (:order f)))
        host (el "records")]
    (render-views!)
    (set! (.-innerHTML host) "")
    (doseq [id ids]
      (let [span (js/document.createElement "span")]
        (set! (.-textContent span) id)
        (.addEventListener span "click"
                           (fn [_]
                             (reset! !editing id)
                             (set! (.-value (el "editor")) (pretty (store/record @!store id)))
                             (set! (.-textContent (el "status")) (str "editing " id))))
        (.appendChild host span)))))

(defn frame!
  "Run the view and show it. Timing is measured here, around the pure runner."
  []
  (let [canvas (el "painting")
        t0 (js/performance.now)
        f (run/frame @!store (view) {:clock #(js/performance.now) :previous @!frame :table @!table})
        t1 (js/performance.now)]
    (swap! !frames assoc @!view-id f)
    (present! canvas (view) (:paintings f))
    (swap! !metrics assoc :frame-ms (- t1 t0) :present-ms (- (js/performance.now) t1)
           :tool-ms (into {} (for [[key ms] (:ms f)] [(pr-str key) (js/Math.round ms)]))
           :resumed (mapv pr-str (:resumed f))
           :statuses (into {} (for [[key s] (:statuses f)] [(pr-str key) s]))
           :caret (:caret f)
           :paintings (mapv (fn [{:keys [run box surface]}] {:run run :box box :size [(:width surface) (:height surface)] :revision (:revision surface)}) (:paintings f))
           :placements (reduce + 0 (map count (vals (:placements f)))))
    (render-readout!)
    (render-records!)))

(defn- on-pointer [e]
  (when-let [f @!frame]
    (let [t0 (js/performance.now)
          x (.-offsetX e) y (.-offsetY e)
          local (run/texel->local (view) x y)
          hit (run/hit @!store (view) f local {:table @!table})]
      (swap! !metrics assoc :pointer {:texel [x y] :local local :hit hit :hit-ms (- (js/performance.now) t0)})
      (when (and @!dragging hit (= (:run hit) (:in (cursor))))
        (let [c (cursor) offset (offset-at hit local)]
          (when (not= offset (:offset c))
            (move-cursor! offset (or (:anchor c) (:offset c))))))
      (render-readout!))))

(defn- editing-text? []
  (contains? #{"TEXTAREA" "INPUT"} (.-tagName (.-activeElement js/document))))

(defn- edit!
  "Keystroke records for the cursor's run, stamped with the stream index,
   the time and the typing view's asserter, and the cursor's new offset →
   the store grows by those records, the cursor moves and loses its anchor;
   then a frame."
  [keys offset]
  (let [v (view) c (cursor) run-id (:in c)]
    (swap! !store (fn [s]
                    (let [s (reduce (fn [s key]
                                      (store/append-key s run-id (assoc key :i (count (:keys (store/record s run-id))) :t (js/Date.now) :by (:by v))))
                                    s keys)
                          n (count (table/fold-keys (:keys (store/record s run-id))))]
                      (store/edit s (:id c) assoc :offset (max 0 (min offset n)) :anchor nil))))
    (let [t0 (js/performance.now)]
      (frame!)
      (swap! !metrics assoc :keys-ms (- (js/performance.now) t0))
      (render-readout!))))

(defn- type-key!
  "A typed character or Enter → inserted at the cursor, replacing a
   selection when one stands."
  [key]
  (let [c (cursor) [from to] (selection c) at (or from (:offset c))]
    (edit! (cond-> [] (and from to) (conj {:key "Delete" :from from :to to}) true (conj (assoc key :at at)))
           (inc at))))

(defn- backspace! []
  (let [c (cursor) [from to] (selection c)]
    (if from
      (edit! [{:key "Delete" :from from :to to}] from)
      (when (pos? (:offset c)) (edit! [{:key "Backspace" :at (:offset c)}] (dec (:offset c)))))))

(defn- on-key [e]
  (when-not (editing-text?)
    (let [k (.-key e) c (cursor) n (count (table/fold-keys (:keys (store/record @!store (:in c)))))]
      (cond
        (and (or (.-ctrlKey e) (.-metaKey e)) (= (.toLowerCase k) "c"))
        (when-let [text (selected-text)]
          (.preventDefault e)
          (swap! !metrics assoc :copied text)
          (when-let [clipboard (.-clipboard js/navigator)] (.catch (.writeText clipboard text) (fn [_] nil)))
          (render-readout!))
        (and (or (.-ctrlKey e) (.-metaKey e)) (= (.toLowerCase k) "z"))
        (do (.preventDefault e)
            (let [n' (count (table/fold-keys (conj (:keys (store/record @!store (:in c))) {:key "Undo"})))]
              (edit! [{:key "Undo"}] (min (:offset c) n'))))
        (or (.-ctrlKey e) (.-metaKey e)) nil
        (= k "Backspace") (do (.preventDefault e) (backspace!))
        (= k "Enter") (do (.preventDefault e) (type-key! {:key "Enter"}))
        (= k "ArrowLeft") (do (.preventDefault e) (move-cursor! (max 0 (dec (:offset c))) (when (.-shiftKey e) (or (:anchor c) (:offset c)))))
        (= k "ArrowRight") (do (.preventDefault e) (move-cursor! (min n (inc (:offset c))) (when (.-shiftKey e) (or (:anchor c) (:offset c)))))
        (= k "ArrowUp") (do (.preventDefault e) (move-line! -1))
        (= k "ArrowDown") (do (.preventDefault e) (move-line! 1))
        (= 1 (.-length k)) (do (.preventDefault e) (type-key! {:ch k}))
        :else nil))))

(defn- on-mouse-down [e]
  (when-let [f @!frame]
    (let [local (run/texel->local (view) (.-offsetX e) (.-offsetY e))
          hit (run/hit @!store (view) f local {:table @!table})]
      (when hit
        (reset! !dragging true)
        (if (= (:run hit) (:in (cursor)))
          (move-cursor! (offset-at hit local) nil)
          ;; the cursor moves into the run under the pointer
          (let [c (cursor)]
            (swap! !store store/edit (:id c) assoc :in (:run hit) :offset (offset-at hit local) :anchor nil)
            (frame!)))))))

(defn- on-mouse-up [_] (reset! !dragging false))

(defn apply-edit!
  "The editor's EDN → the store, then a frame. A record keeps its id; the
   store logs what it replaced."
  []
  (try
    (let [record (reader/read-string (.-value (el "editor")))]
      (when-not (and (map? record) (string? (:id record)))
        (throw (ex-info "A record needs a string :id" {:record record})))
      (swap! !store store/put record)
      (frame!)
      (set! (.-textContent (el "status")) (str "applied " (:id record))))
    (catch :default e
      (set! (.-textContent (el "status")) (str "refused: " (.-message e))))))

(defn bench
  "Where a ring's paint goes: the surface copy alone, the paint call alone,
   and the same paint through the executor. Diagnostic only."
  []
  (let [f @!frame run-id (first (:order f)) p (:surface (first (:paintings f)))
        rings (get-in f [:runs ["layout@1" run-id] :results :outline :rings])
        ring (first rings)
        region {:path (:path (source/build {:kind :anchors :contours [ring]} {})) :rule :nonzero}
        t (fn [n g] (let [t0 (js/performance.now)] (dotimes [_ n] (g)) (/ (- (js/performance.now) t0) n)))
        paint-args {:surface p :region region :rgba [0 0 0 1] :opacity 1 :blend :source-over}
        one {:program {:steps [{:out :painted :op :paint
                                :args {:surface [:get :painting] :region [:get :region] :rgba [0 0 0 1] :opacity 1 :blend :source-over}}]
                       :return {:p [:get :painted]}}
             :roots {}}]
    (pr-str {:surface [(:width p) (:height p)]
             :slice-ms (t 20 #(.slice (:data p)))
             :paint-direct-ms (t 20 #(path-surface/paint paint-args {:at 0 :step :b}))
             :paint-via-executor-ms (t 20 #(executor/run one {:painting p :region region} @!table))
             :rings (count rings)})))

(defn- glyph-centre
  "A run id and a placement index → the texel at the centre of that glyph's
   rect, as \"x y\"; for the driver to point at real glyphs."
  [run i]
  (let [{:keys [rect]} (nth (get-in @!frame [:placements run]) i)
        [x y w h] rect
        [tx ty] (run/local->texel (view) [(+ x (/ w 2)) (+ y (/ h 2))])]
    (str (js/Math.round tx) " " (js/Math.round ty))))

(defn- load-fonts!
  "Every font record the view pins that names a :source → its file fetched
   and read (text/truetype.cljc), the completed record put back into the
   store, the table rebuilt over the fonts; then k. A font that fails to
   load is reported and the box table stands."
  [k]
  (let [pinned (for [[_ id] (:pins (view)) :let [r (store/record @!store id)] :when (and (= :font (:kind r)) (:source r))] r)]
    (if (empty? pinned)
      (k)
      (-> (js/Promise.all
           (clj->js (for [r pinned]
                      (-> (js/fetch (:source r))
                          (.then (fn [response]
                                   (when-not (.-ok response) (throw (js/Error. (str (:source r) " " (.-status response)))))
                                   (.arrayBuffer response)))
                          (.then (fn [buf] #js [r (js/Uint8Array. buf)]))))))
          (.then (fn [pairs]
                   (let [fonts (into {} (for [pair pairs
                                              :let [r (aget pair 0) bytes (aget pair 1) parsed (truetype/parse bytes)]]
                                          (do (swap! !store store/put (merge r (truetype/metrics parsed) {:digest (truetype/digest bytes)}))
                                              [(:id r) parsed])))]
                     (reset! !table (table/with-fonts fonts))
                     (k))))
          (.catch (fn [e]
                    (set! (.-textContent (el "status")) (str "font not loaded: " (.-message e)))
                    (k)))))))

(def store-key "softland/view/store")

(defn- load-store
  "The store this browser saved last, or nil. Whatever it holds is data;
   a record that no longer parses loses the whole saved store, by design."
  []
  (try (when-let [s (.getItem js/localStorage store-key)] (reader/read-string s))
       (catch :default _ nil)))

(defn- save-store! [s]
  (try (.setItem js/localStorage store-key (pr-str s))
       (catch :default _ nil)))

(defonce !started (atom false))

(defn ^:export start!
  "Wire the canvas, the pointer, the keyboard and the editor once, then frame."
  []
  (when (compare-and-set! !started false true)
   (let [canvas (el "painting")]
    (when-let [saved (load-store)] (reset! !store saved))
    (add-watch !store ::persist (fn [_ _ _ s] (save-store! s)))
    (.addEventListener canvas "mousemove" on-pointer)
    (.addEventListener canvas "mousedown" on-mouse-down)
    (.addEventListener js/window "mouseup" on-mouse-up)
    (.addEventListener js/window "keydown" on-key)
    (.addEventListener (el "apply") "click" (fn [_] (apply-edit!)))
    (.addEventListener (el "views") "change" (fn [e] (reset! !view-id (.-value (.-target e))) (frame!)))
    (set! js/window.softland
          #js {:metrics (fn [] (pr-str @!metrics))
               :frame (fn [] (frame!) (pr-str (:statuses @!metrics)))
               :record (fn [id] (pr-str (store/record @!store id)))
               :put (fn [edn] (swap! !store store/put (reader/read-string edn)) (frame!) (pr-str (:statuses @!metrics)))
               :log (fn [] (pr-str (map #(dissoc % :previous) (:log @!store))))
               :bench bench
               :glyphCentre glyph-centre
               :stand (fn [id] (reset! !view-id id) (frame!) (pr-str (:statuses @!metrics)))
               :cursor (fn [] (pr-str (cursor)))
               :text (fn [run] (apply str (table/fold-keys (:keys (store/record @!store run)))))
               :reset (fn [] (.removeItem js/localStorage store-key) (reset! !store records/store) (reset! !frames {}) (frame!) "reset")
               :ready false})
    (load-fonts! (fn [] (frame!) (set! (.-ready js/window.softland) true))))))
