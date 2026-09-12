(require '[app.client.path.records :as records] '[app.client.path.component :as c]
         '[app.client.path.geometry :as g] '[app.client.path.curves :as curves] '[app.client.path.pack :as pack]
         '[app.client.harness.path-fixtures :as fx] '[app.client.engine.executor :as ex])
(println "== Codex lane, pure layer ==")
(defn draw-rec [id samples stroke & [settings]]
  (records/construct {:identity {:id id :revision 1} :construction records/draw
                      :source {:samples samples} :settings (or settings {:interpolation :line})
                      :paint {:stroke stroke}}))
(defn samples [rows] (mapv (fn [i [x y p]] {:id i :position [x y] :pressure p :width (* 16.0 p)}) (range) rows))
(defn try* [f] (try (f) (catch Exception e (str "THROWS " (ex-message e) " " (ex-data e)))))
;; A. the Z
(let [u (records/construct (fx/z-record :union)) a (records/construct (fx/z-record :accumulate))]
  (println "A. Z union regions:" (count (c/regions u 1.0)) " dabs:" (count (c/regions a 1.0)) " classify (64,64):" (c/classify u [64.0 64.0])))
;; B. blob
(def blob (draw-rec :blob (samples [[10.0 50.0 0.1] [12.0 50.0 1.0]]) {:tip :round-nib :width :knot :overlap :union :color [0 0 0 1]}))
(println "B. blob: |ra-rb|=7.2 >= piece 2 → the big disc (r 8 at (12,50))")
(doseq [pt [[19.0 50.0] [18.5 50.0] [12.0 57.0] [12.0 43.5] [5.0 50.0] [4.5 50.0] [12.0 50.0]]]
  (println "   " pt "→" (c/classify blob pt) " dist to (12,50) =" (Math/hypot (- (pt 0) 12.0) (- (pt 1) 50.0))))
;; C. nonlinear 40 p^2 on one straight segment
(def nonlin (draw-rec :nonlin (samples [[0.0 0.0 0.1] [100.0 0.0 1.0]])
              {:tip :round-nib :width [:* 40.0 [:get :pressure] [:get :pressure]] :overlap :union :color [0 0 0 1]}))
(println "C. nonlinear 40p²: at x=50 true radius 6.05, interpolation 10.1")
(doseq [y [5.5 6.0 6.1 6.5 8.0 9.5 10.2]] (println "    (50," y ") →" (c/classify nonlin [50.0 y])))
(let [flat (g/flatten-stroke (:path/value nonlin) (get-in nonlin [:path/paint :stroke]) 0.05 1.0)]
  (println "    flattened points on the line:" (count (:points (first flat)))))
(let [rows (curves/quadratics (:path (first (c/regions nonlin 1.0))) 0.05)]
  (println "    boundary distance at (50,0):" (g/boundary-distance rows :nonzero [50.0 0.0] 0.05)))
(let [dabs (c/regions (assoc-in nonlin [:path/paint :stroke] {:tip :round-nib :width [:* 40.0 [:get :pressure] [:get :pressure]] :overlap :accumulate :spacing 50.0 :color [0 0 0 1]}) 1.0)]
  (println "    as dabs (spacing 50): count" (count dabs)))
;; D. a curved stroke: Catmull-Rom through the same 70 limaçon-like samples? use a 12-sample loop with width 10p
(def limacon (mapv (fn [i] (let [t (* 2 Math/PI (/ i 70.0)) r (+ 30 (* 25 (Math/cos t)))]
                              [(+ 64 (* r (Math/cos t))) (+ 64 (* r (Math/sin t))) (+ 0.5 (* 0.5 (Math/sin (* 3 t))))])) (range 70)))
(def curved (draw-rec :curved (mapv (fn [i [x y p]] {:id i :position [x y] :pressure p :width (* 10.0 p)}) (range) limacon)
              {:tip :round-nib :width :knot :overlap :union :color [0 0 0 1]} {:interpolation :catmull-rom :streamline 0.35}))
(let [regs (c/regions curved 1.0) reg (first regs)
      flat (g/flatten-stroke (:path/value curved) (get-in curved [:path/paint :stroke]) 0.05 1.0)]
  (println "D. 70-sample limaçon, Catmull-Rom, width 10p, geometry tolerance .05: flattened points" (count (:points (first flat)))
           " capsule loops" (count (:subpaths (:path reg))))
  (doseq [sc [0.5 3.0 30.0 300.0]]
    (let [p (pack/region reg sc)]
      (println "    scale" sc "→ quads" (count (:rows p)) " bands" (:glyph p) " curve texture rows(4096 wide)" (Math/ceil (/ (count (:curves p)) (* 4096 4)))))))
(let [p (pack/region (first (c/regions (records/construct (fx/z-record :accumulate)) 1.0)) 3.0)]
  (println "    one dab at scale 3: quads" (count (:rows p))))
(let [p (pack/region (first (c/regions (records/construct (fx/z-record :union)) 1.0)) 3.0)]
  (println "    the Z union at scale 3: quads" (count (:rows p)) " glyph" (:glyph p)))
;; F. what a record can say
(def z (records/construct (fx/z-record :union)))
(doseq [[label stroke] [["open Z, miter join" (assoc fx/z-stroke :join :miter)]
                        ["open Z, butt cap" (assoc fx/z-stroke :cap :butt)]
                        ["open Z, dash" (assoc fx/z-stroke :dash [10 10])]
                        ["open Z, ribbon tip" (assoc fx/z-stroke :tip :ribbon)]
                        ["open Z, align inside" (assoc fx/z-stroke :align :inside)]]]
  (println "F." label "→" (try* #(let [r (c/regions (records/construct (assoc-in (fx/z-record :union) [:paint :stroke] stroke)) 1.0)] (str "ok, " (count r) " region(s)")))))
;; the bench's draw-tool record fields: fit, taper, simulate-pressure, streamline
(println "   samples capability options:" (try* #(ex/execute records/draw {:source {:samples (samples [[0 0 0.5] [10 0 0.5]])} :settings {:streamline 0.35 :interpolation :catmull-rom :fit 0.9 :taper-end 22}} records/capabilities)))
(println "   (fit/taper keys are ignored, not refused:" (:keys (ex-data (try (ex/execute records/draw {:source {:samples []} :settings {:fit 0.9}} records/capabilities) (catch Exception e e)))) ")")
;; G. tools as data: see probe-codex-2.clj (the first draft of this section dropped the stroke from the paint and hit a missing binding)
(System/exit 0)
