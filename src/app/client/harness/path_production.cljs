(ns app.client.harness.path-production
  "Pixel receipts for the production path boundary. The parent supplies
   the repo's capture function and a fresh-system capture. Every check
   reads RGBA bytes; images and numeric samples travel in the dump result."
  (:require [app.client.engine.executor :as executor]
            [app.client.path.component :as component]
            [app.client.path.construction :as construction]
            [app.client.path.records :as records]
            [app.client.path.value :as value]
            [app.client.harness.shared :as shared]))

(defn- alpha [bytes [x y]] (/ (nth (shared/pixel-rgba bytes x y) 3) 255.0))
(defn- near? [a b] (< (js/Math.abs (- a b)) (/ 2.0 255.0)))
(defn- image-record [label bytes]
  {:file (str "gpu-path-production-" label ".png")
   :png-data-url (shared/opaque-png-data-url bytes)})

(defn- capture-without-recipes! [capture record view group]
  ;; prepare is synchronous before the capture's readback Promise. A
  ;; renderer entering the executor here fails the browser suite.
  (with-redefs [executor/execute (fn [& _] (throw (ex-info "renderer ran a recipe" {})))]
    (capture record view group)))

(defn- border-check! [capture]
  (let [border (-> (construction/construct records/border)
                   (update :path/paint dissoc :fill)
                   (assoc-in [:path/paint :stroke :color] [1 1 1 1]))
        specs (concat
               (for [zoom [0.01 0.1 1.0 8.0 10.0 100.0 1000.0]]
                 {:label (str "border-z" zoom) :zoom zoom :pan [0.0 0.0] :group 0})
               [{:label "snap-fraction" :zoom 1.0 :pan [0.25 0.5] :group 0}
                {:label "snap-integer-shift" :zoom 1.0 :pan [5.25 -2.5] :group 0}
                {:label "screen-z1" :zoom 1.0 :pan [0.0 0.0] :group 19}
                {:label "screen-z2" :zoom 2.0 :pan [1.25 20.5] :group 19}])]
    (shared/promise-mapv
      (fn [{:keys [label zoom pan group]}]
        (let [screen? (= 19 group)
              scale (if screen? 1.0 zoom)
              record (update border :path/value value/map-points #(value/scale % (/ 1.0 scale)))
              left (js/Math.round (+ 24.0 (if screen? 0.0 (first pan))))
              points [[(dec left) 60] [left 60] [(inc left) 60]]]
          (.then (capture-without-recipes! capture record {:zoom zoom :pan pan} group)
                 (fn [{:keys [bytes frame]}]
                   (let [samples (mapv #(alpha bytes %) points)]
                     {:case label :zoom zoom :pan pan :group group
                      :normalization "path coordinates divided by projected scale; device width remains 1"
                      :points points :alpha samples :frame (select-keys frame [:derivations :packs :instance-writes])
                      :images [(image-record label bytes)]
                      :pass? (every? true? (map near? samples [0.0 1.0 0.0]))}))))) specs)))

(defn- recipe-check! [capture cold-capture]
  (let [calls (atom 0)
        construct (fn [record] (swap! calls inc) (construction/construct record))
        before (construct records/harness-z)
        after (construct (assoc records/harness-z :path/revision 2
                                 :path/construction (construction/default-construction records/z-as-dabs)))
        view {:zoom 1.0 :pan [0.0 0.0]}]
    (-> (capture-without-recipes! capture before view 0)
        (.then (fn [a]
                 (.then (capture-without-recipes! capture after view 0)
                        (fn [b]
                          (.then (capture-without-recipes! cold-capture after view 0)
                                 (fn [c]
                                   (let [samples (mapv #(alpha (:bytes %) [64 64]) [a b c])]
                                     {:source-edits @calls :alpha samples
                                      :regions (mapv #(count (:regions (component/regions % {}))) [before after])
                                      :images (mapv (fn [label result] (image-record label (:bytes result)))
                                                    ["recipe-before" "recipe-retained" "recipe-fresh"] [a b c])
                                      :pass? (and (= 2 @calls) (near? (first samples) 0.62)
                                                  (every? #(near? % 0.8556) (rest samples)))}))))))))))

(defn- membership-check! [capture]
  (let [z (construction/construct records/harness-z)
        nonlinear (construction/construct
                    {:path/material-id :nonlinear :path/revision 1
                     :path/tool {:size 20 :fit :polyline :streamline 0}
                     :path/source {:kind :pen :samples [[20.5 30.5 0.0] [40.5 30.5 1.0]]}
                     :path/paint {:stroke {:width [:* [:get :size] [:pow [:get :p] 2]] :color [1 1 1 0.62]}}})
        large-disc (assoc nonlinear :path/value
                          {:subpaths [{:start [12.5 50.5] :closed? false
                                       :segments [{:kind :line :p [12.5 50.5]}]
                                       :knots [{:id 0 :width 1.6} {:id 1 :width 16.0}]}]})
        large-disc (assoc-in large-disc [:path/paint :stroke :width] :knot)
        mask (construction/construct
               {:path/material-id :mask :path/revision 1 :path/source {:kind :custom :width 32.25}
                :path/paint {:fill {:rule :nonzero :color [1 1 1 1]}}
                :path/construction {:steps [] :return
                                     {:subpaths [{:start [0 0] :closed? true
                                                  :segments [{:kind :line :p [[:* 2 [:get :source :width]] 0]}
                                                             {:kind :line :p [[:* 2 [:get :source :width]] 128]}
                                                             {:kind :line :p [0 128]}]}]}}})
        clipped (assoc-in z [:path/paint :clip] {:path (:path/value mask) :rule :nonzero})
        empty-clip (assoc-in z [:path/paint :clip] {:path {:subpaths []} :rule :nonzero})
        specs [["nonlinear-union" nonlinear [[30 35] [40 30]] [0.0 0.62]]
               ["containing-disc" large-disc [[19 50] [21 50]] [0.62 0.0]]
               ["returned-mask" clipped [[64 64] [65 64]] [0.31 0.0]]
               ["empty-clip" empty-clip [[64 64] [26 28]] [0.0 0.0]]]]
    (shared/promise-mapv
      (fn [[label record points expected]]
        (.then (capture-without-recipes! capture record {:zoom 1.0 :pan [0.0 0.0]} 0)
               (fn [{:keys [bytes]}]
                 (let [samples (mapv #(alpha bytes %) points)]
                   {:case label :points points :alpha samples :expected expected
                    :images [(image-record label bytes)]
                    :pass? (every? true? (map near? samples expected))})))) specs)))

(defn- taper-check! [capture]
  (let [tapered (construction/construct records/draw-tool)
        full (construction/construct (assoc-in records/draw-tool [:path/tool :taper-end] 0))
        view {:zoom 1.0 :pan [0.0 0.0]}]
    (-> (capture-without-recipes! capture tapered view 0)
        (.then (fn [a]
                 (.then (capture-without-recipes! capture full view 0)
                        (fn [b]
                          (let [changes (vec (for [y (range shared/canvas-size) x (range shared/canvas-size)
                                                   :let [av (alpha (:bytes a) [x y]) bv (alpha (:bytes b) [x y])]
                                                   :when (> (- bv av) 0.5)]
                                               {:point [x y] :tapered av :untapered bv}))]
                            {:reduced-pixels (count changes) :sample (vec (take 8 changes))
                             :tripwire {:point [119 36] :tapered (alpha (:bytes a) [119 36])
                                        :untapered (alpha (:bytes b) [119 36])}
                             :images [(image-record "draw-tapered" (:bytes a)) (image-record "draw-untapered" (:bytes b))]
                             :pass? (and (near? (alpha (:bytes a) [119 36]) 0.0)
                                         (near? (alpha (:bytes b) [119 36]) 0.85))}))))))))

(defn run-checks! [capture cold-capture]
  (-> (border-check! capture)
      (.then (fn [border] (.then (recipe-check! capture cold-capture) #(hash-map :border border :recipe %))))
      (.then (fn [state] (.then (membership-check! capture) #(assoc state :membership %))))
      (.then (fn [state] (.then (taper-check! capture) #(assoc state :taper %))))
      (.then (fn [{:keys [border recipe membership taper] :as state}]
               (assoc state :pass? (and (every? :pass? border) (:pass? recipe)
                                       (every? :pass? membership) (:pass? taper)))))))
