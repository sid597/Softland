(require '[app.client.path.component :as c] '[app.client.path.records :as r]
         '[app.client.path.pack :as pack] '[app.client.path.frame :as frame]
         '[app.client.path.stroke :as stroke] '[app.client.path.source :as source]
         '[clojure.pprint :as pp])
(println "== Claude lane, pure layer ==")
(defn rec [id source paint & [tool]]
  (cond-> {:path/material-id id :path/revision 1 :path/source source :path/paint paint}
    tool (assoc :path/tool tool)))
;; A. the definer's Z, as the handoff says
(println "A. Z regions:" (map :kind (:regions (c/run r/harness-z {}))) " dabs:" (count (:regions (c/run r/z-as-dabs {}))))
(println "   classify Z at (64,64):" (c/classify r/harness-z [64.0 64.0]))
;; B. blob: two samples 2 units apart, pressure 0.1 and 1.0, width 16p (radii 0.8 and 8)
(def blob (rec :blob {:kind :pen :samples [[10.0 50.0 0.1] [12.0 50.0 1.0]]}
               {:stroke {:tip :nib :width :knot :color [0 0 0 1]}}))
(println "B. blob: |ra-rb|=7.2 >= piece 2 → the big disc (r 8 at (12,50)) should be the region")
(doseq [pt [[19.0 50.0] [18.5 50.0] [12.0 57.0] [12.0 43.5] [5.0 50.0] [4.5 50.0] [12.0 50.0]]]
  (println "   " pt "→" (c/classify blob pt) " dist to (12,50) =" (Math/hypot (- (pt 0) 12.0) (- (pt 1) 50.0))))
(let [run (c/run blob {})
      reg (first (:regions run))]
  (println "   blob skin subpaths:" (count (:subpaths (:path reg))) " segments:" (count (:segments (first (:subpaths (:path reg)))))))
;; C. nonlinear width where a pixel turns: one line (0,0)→(100,0), p .1→1, width 40 p^2
(def nonlin (rec :nonlin {:kind :pen :samples [[0.0 0.0 0.1] [100.0 0.0 1.0]]}
                 {:stroke {:tip :nib :width "40*p*p" :color [0 0 0 1]}}
                 {:size 40}))
(println "C. nonlinear 40p²: at x=50 true radius = 20*.55² = 6.05; interpolating widths would give 10.1")
(doseq [pt [[50.0 6.5] [50.0 8.0] [50.0 9.5] [50.0 5.5]]]
  (println "   " pt "→" (c/classify nonlin pt)))
(println "   reads:" (keys (:reads (c/run nonlin {}))))
;; D. curved stroke: the draw tool's limaçon samples through the pen source with Catmull-Rom, width 10p; count curves
(def draw (c/run r/draw-tool {}))
(println "D. draw tool (records/draw-tool): ok?" (:ok? draw) " regions" (map :kind (:regions draw))
         " pieces" (:pieces (first (:regions draw))) " arcs" (:arcs (first (:regions draw))))
(let [reg (first (:regions draw))
      p3 (:pack (pack/pack-region (:path reg) (pack/bucket-tolerance (pack/scale-bucket 3.0)) {}))]
  (println "   packed at scale 3 (bucket" (pack/scale-bucket 3.0) "): curves" (:count p3) " bands" (:bands p3) (:rows p3) (:cols p3)))
(let [reg (first (:regions draw))]
  (doseq [sc [0.5 3.0 30.0 300.0]]
    (let [b (pack/scale-bucket sc) p (:pack (pack/pack-region (:path reg) (pack/bucket-tolerance b) {}))]
      (println "   scale" sc "bucket" b "tolerance" (pack/bucket-tolerance b) "→ curves" (:count p)))))
(println "   pen tool:" (let [run (c/run r/pen-tool {})] [(:ok? run) (map :kind (:regions run))]))
;; E. the hashed pack key: find two different outlines with the same region-key
(println "E. frame/region-key uses (hash path): search for a collision among random one-line outlines")
(let [mk (fn [i] {:subpaths [{:closed? true :start [(double (mod (* i 7919) 1000)) (double (mod (* i 104729) 1000))]
                              :segments [{:type :line :to [(double (mod (* i 31) 997)) (double i)]}]}]})
      found (loop [i 0 seen {}]
              (if (> i 3000000) nil
                  (let [p (mk i) k (frame/region-key {:path p :rule :nonzero})]
                    (if-let [j (get seen k)]
                      [j i (mk j) p k]
                      (recur (inc i) (assoc seen k i))))))]
  (if found
    (let [[j i pj pi k] found]
      (println "   collision after" i "outlines: key" k)
      (println "   outline" j ":" (pr-str pj))
      (println "   outline" i ":" (pr-str pi))
      (println "   equal?" (= pj pi) " same pack-key at bucket 0?" (= (frame/pack-key {:path pj :rule :nonzero} 0) (frame/pack-key {:path pi :rule :nonzero} 0))))
    (println "   no collision in 3M (JVM hash)")))
;; F. capabilities a record can say: miter join on an open path, butt cap, dash, ribbon
(doseq [[label paint] [["open Z, miter join" {:stroke {:tip :nib :width :knot :join :miter :color [0 0 0 1]}}]
                       ["open Z, butt cap" {:stroke {:tip :nib :width :knot :cap :butt :color [0 0 0 1]}}]
                       ["open Z, dash [10 10]" {:stroke {:tip :nib :width :knot :dash [10.0 10.0] :color [0 0 0 1]}}]
                       ["open Z, ribbon" {:stroke {:tip :ribbon :width :knot :color [0 0 0 1]}}]]]
  (let [run (c/run (assoc r/harness-z :path/paint paint) {})]
    (println "F." label "→ ok?" (:ok? run) " subpaths" (count (:subpaths (:path (first (:regions run))))) (when-not (:ok? run) (:log run)))))
;; G. a tool as data: a custom construction that fills the skin and reuses it as a clip in another record
(def skin-fill (assoc r/harness-z :path/construction
                 {:steps [{:out "path" :op :path/source :source "source" :tool "tool"}
                          {:out "skin" :op :path/envelope :path "path" :tool "tool" :stroke "paint.stroke.geometry"}
                          {:out "fill" :op :path/fill-region :path "skin.0.path" :rule :even-odd}]
                  :return {:path "path" :regions ["fill"]}}))
(println "G. custom construction (fill the skin even-odd): regions" (map :kind (:regions (c/run skin-fill {})))
         " classify (64,64):" (c/classify skin-fill [64.0 64.0]) " (union gives" (c/classify r/harness-z [64.0 64.0]) ")")
(let [skin (:path (first (:regions (c/run r/harness-z {}))))
      shape (assoc-in r/holed-concave [:path/paint :clip] {:path skin :rule :nonzero})
      run (c/run shape {})]
  (println "   holed-concave clipped by the Z's returned skin: clip present?" (some? (:clip run)) " reads" (keys (:reads run))
           " classify (90,90):" (c/classify shape [90.0 90.0]) " (60,40):" (c/classify shape [60.0 40.0])))
(let [run (c/run (assoc-in r/harness-z [:path/construction] {:steps [{:out "face" :op :geometry/arrange :curves "source"}] :return {:path "face" :regions []}}) {})]
  (println "   an op the table lacks → ok?" (:ok? run) " missing" (:missing run)))
(System/exit 0)
