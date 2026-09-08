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
            [app.client.view.records :as records]
            [app.client.view.store :as store]
            [app.client.view.run :as run]
            [app.client.view.table :as table]
            [cljs.reader :as reader]
            [cljs.pprint :as pprint]
            [clojure.string :as str]))

(defonce !store (atom records/store))
(defonce !view-id (atom "view-1"))
(defonce !frame (atom nil))
(defonce !metrics (atom {}))
(defonce !editing (atom nil))

(defn- el [id] (js/document.getElementById id))
(defn- view [] (store/record @!store @!view-id))

(defn present!
  "Surface value → the canvas: premultiplied linear RGBA32F composed over
   white, encoded as straight sRGB 8-bit. The CPU runner's own display."
  [canvas {:keys [width height data]}]
  (when-not (and (= width (.-width canvas)) (= height (.-height canvas)))
    (set! (.-width canvas) width)
    (set! (.-height canvas) height))
  (let [ctx (.getContext canvas "2d")
        img (.createImageData ctx width height)
        px (.-data img)]
    (dotimes [i (* width height)]
      (let [j (* 4 i) k (- 1.0 (aget data (+ j 3)))]
        (dotimes [c 3]
          (aset px (+ j c) (js/Math.round (* 255 (color/linear->srgb-channel (min 1.0 (+ (aget data (+ j c)) k)))))))
        (aset px (+ j 3) 255)))
    (.putImageData ctx img 0 0)))

(defn- pretty [x] (with-out-str (pprint/pprint x)))

(defn- render-readout! []
  (let [{:keys [frame-ms present-ms pointer statuses keys-ms tool-ms]} @!metrics
        v (view)]
    (set! (.-textContent (el "readout"))
          (str "view " (:id v) " by " (:by v) " zoom " (:zoom v) " subject " (pr-str (:subject v)) "\n"
               "frame " (when frame-ms (.toFixed frame-ms 1)) " ms · present " (when present-ms (.toFixed present-ms 1)) " ms"
               (when keys-ms (str " · last key " (.toFixed keys-ms 1) " ms")) "\n"
               "tools " (pr-str statuses) "\n"
               "tool ms " (pr-str tool-ms) "\n"
               "pointer " (pr-str (:local pointer)) " → " (pretty (:hit pointer))))))

(defn- render-records! []
  (let [v (view) f @!frame
        ids (distinct (concat [(:id v)] (:tools v) [(:hit-tool v)] (vals (:pins v))
                              (get-in f [:runs "query@1" :results :runs :ids])))
        host (el "records")]
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
        f (run/frame @!store (view) [(.-width canvas) (.-height canvas)] {:clock #(js/performance.now)})
        t1 (js/performance.now)]
    (reset! !frame f)
    (when (:painting f) (present! canvas (:painting f)))
    (swap! !metrics assoc :frame-ms (- t1 t0) :present-ms (- (js/performance.now) t1)
           :tool-ms (into {} (for [[id ms] (:ms f)] [id (js/Math.round ms)]))
           :statuses (:statuses f) :caret (:caret f)
           :placements (count (get-in f [:placements :items])))
    (render-readout!)
    (render-records!)))

(defn- on-pointer [e]
  (when-let [p (:painting @!frame)]
    (let [t0 (js/performance.now)
          x (.-offsetX e) y (.-offsetY e)
          local (surface/to-point p x y)
          r (run/hit @!store (view) @!frame local)]
      (swap! !metrics assoc :pointer {:texel [x y] :local local :hit (get-in r [:results :hit])
                                      :status (:status r) :hit-ms (- (js/performance.now) t0)})
      (render-readout!))))

(defn- editing-text? []
  (contains? #{"TEXTAREA" "INPUT"} (.-tagName (.-activeElement js/document))))

(defn- append-key!
  "One keystroke → the cursor's run grows by one record and the cursor moves
   to the end of the folded text; then a frame."
  [key]
  (let [v (view) cursor (store/record @!store (get-in v [:pins :cursor])) run-id (:in cursor)]
    (swap! !store (fn [s]
                    (let [s (store/append-key s run-id (assoc key :i (count (:keys (store/record s run-id))) :t (js/Date.now)))
                          n (count (table/fold-keys (:keys (store/record s run-id))))]
                      (store/edit s (:id cursor) assoc :offset n))))
    (let [t0 (js/performance.now)]
      (frame!)
      (swap! !metrics assoc :keys-ms (- (js/performance.now) t0))
      (render-readout!))))

(defn- on-key [e]
  (when-not (editing-text?)
    (let [k (.-key e)]
      (cond
        (= k "Backspace") (do (.preventDefault e) (append-key! {:key "Backspace"}))
        (= k "Enter") (do (.preventDefault e) (append-key! {:key "Enter"}))
        (and (= 1 (.-length k)) (not (.-ctrlKey e)) (not (.-metaKey e))) (do (.preventDefault e) (append-key! {:ch k}))
        :else nil))))

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
  (let [f @!frame p (:painting f)
        ring (first (get-in f [:runs "layout@1" :results :outline :rings]))
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
             :paint-via-executor-ms (t 20 #(executor/run one {:painting p :region region} table/table))
             :rings (count (get-in f [:runs "layout@1" :results :outline :rings]))})))

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
    (.addEventListener js/window "keydown" on-key)
    (.addEventListener (el "apply") "click" (fn [_] (apply-edit!)))
    (set! js/window.softland
          #js {:metrics (fn [] (pr-str @!metrics))
               :frame (fn [] (frame!) (pr-str (:statuses @!metrics)))
               :record (fn [id] (pr-str (store/record @!store id)))
               :put (fn [edn] (swap! !store store/put (reader/read-string edn)) (frame!) (pr-str (:statuses @!metrics)))
               :log (fn [] (pr-str (map #(dissoc % :previous) (:log @!store))))
               :bench bench
               :reset (fn [] (.removeItem js/localStorage store-key) (reset! !store records/store) (frame!) "reset")
               :ready true})
    (frame!))))
