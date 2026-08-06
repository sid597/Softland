(ns app.client.substrate.chrome-derive-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.substrate.chrome-derive :as chrome]
            [app.client.workspace.selection :as selection]))

(def frame
  {:targets-by-address
   {:block [{:vi :vi/block :container 1 :container-idx 1
             :bounds {:x 0.0 :y 0.0 :w 40.0 :h 20.0}
             :family :render.family/rect}]
    :fixture [{:vi :vi/fixture :container 2 :container-idx 2
               :bounds {:x 10.0 :y 20.0 :w 30.0 :h 40.0}
               :family :render.family/path}]
    :edge [{:vi :vi/edge :container 3 :container-idx 3
            :bounds {:x 0.0 :y 0.0 :w 100.0 :h 100.0}
            :family :render.family/connector}]}})

(def effective-a
  {1 {:affine [1.0 0.0 0.0 1.0 0.0 0.0] :flags 0}
   2 {:affine [2.0 0.0 0.0 2.0 100.0 40.0] :flags 0}
   3 {:affine [1.0 0.0 0.0 1.0 0.0 0.0] :flags 0}})

(deftest incremental-oracle-and-proportionality-tripwire
  (let [block {:vi :vi/block :address :block}
        fixture {:vi :vi/fixture :address :fixture}
        edge {:vi :vi/edge :address :edge}
        transitions [{:op :toggle :identity block}
                     {:op :toggle :identity fixture}
                     {:op :toggle :identity edge}
                     {:op :toggle :identity block}]
        states (rest (reductions #(chrome/apply-transition %1 %2 frame nil nil)
                                 (chrome/empty-state) transitions))]
    (doseq [state states]
      (is (= (:slots state)
             (chrome/full-recompute-oracle
              {:selection (:selection state) :store-frame frame}))
          "incremental state fences against an independent full recompute"))
    (is (= [1 1 0 1]
           (mapv #(get-in % [:frame-receipt :chrome-derives]) states))
        "one target transition changes exactly one visual slot; edge membership has none")
    (let [fixture-state (second states)
          fixture-slot (get-in fixture-state [:slots [:chrome/target :vi/fixture :fixture]])]
      (is (= 5 (count (:nodes fixture-slot))))
      (is (= #{:nw :ne :sw :se}
             (into #{} (keep #(get-in % [:data :chrome/material :chrome/corner]))
                   (:nodes fixture-slot))))
      (doseq [node (:nodes fixture-slot)]
        (is (= :render.family/chrome (get-in node [:data :render/family]))
            "every chrome node is explicit; none can fall through to universal hit")))))

(deftest transform-value-diff-and-handle-slop-tripwire
  (let [selected (selection/marquee-commit
                  (selection/empty-selection)
                  #{{:vi :vi/block :address :block}
                    {:vi :vi/fixture :address :fixture}})
        state (chrome/apply-selection (chrome/empty-state) selected frame nil nil)
        baseline (chrome/follow-transforms state effective-a)
        moved-effective (assoc-in effective-a [2 :affine]
                                  [2.0 0.0 0.0 2.0 120.0 40.0])
        moved (chrome/follow-transforms baseline moved-effective)
        static (chrome/follow-transforms moved moved-effective)
        fixture-slot (get-in state [:slots [:chrome/target :vi/fixture :fixture]])
        handle (first (filter #(= :handle
                                  (get-in % [:data :chrome/material :chrome/form]))
                              (:nodes fixture-slot)))]
    (is (= 0 (get-in baseline [:frame-receipt :chrome-transform-updates])))
    (is (= #{[:chrome/target :vi/fixture :fixture]} (:transform-writes moved)))
    (is (= 1 (get-in moved [:frame-receipt :chrome-transform-updates])))
    (is (= 0 (get-in static [:frame-receipt :chrome-transform-updates])))
    (chrome/set-live-camera-provider! (constantly {:zoom 8.0}))
    (chrome/set-live-effective-provider! (constantly effective-a))
    (let [center (get-in handle [:data :chrome/hit-center-local])
          radius (+ (/ 5.0 16.0) (/ 6.0 16.0))]
      (is (= [chrome/broad-phase-radius chrome/broad-phase-radius] center))
      (is (= {:x 10.0 :y 20.0
              :w (* 2.0 chrome/broad-phase-radius)
              :h (* 2.0 chrome/broad-phase-radius)}
             (:bounds handle))
          "root-relative handle bounds put the true corner at the broad-phase center")
      (is (chrome/handle-hit? handle (mapv + center [radius 0.0])))
      (is (not (chrome/handle-hit? handle (mapv + center [(+ radius 0.001) 0.0]))))
      (is (= 0.375 (chrome/handle-slop-local-radius 8.0 (get effective-a 2)))
          "slop divides by both zoom and the scaled fixture container"))
    (chrome/set-live-effective-provider! (constantly {}))
    (is (false? (chrome/handle-hit? handle [chrome/broad-phase-radius
                                            chrome/broad-phase-radius]))
        "missing live effective transform fails closed instead of throwing")))

(deftest gesture-alignment-value-identity-tripwire
  (let [gesture {:id :g1 :marquee {:x 0.0 :y 0.0 :w 30.0 :h 20.0}}
        snap {:alignments #{[:x :edge 100.0]}
              :guides [{:axis :x :kind :edge :candidate-coordinate 100.0
                        :from [100.0 0.0] :to [100.0 80.0]}]
              :ticks [{:axis :x :at [70.0 40.0] :gap 10.0}]}
        slot (chrome/gesture-slot gesture snap)]
    (is (= 3 (count (:nodes slot))))
    (is (= (:alignments snap) (:alignment-set slot)))
    (is (= #{:marquee :guide-line :gap-tick}
           (into #{} (map #(get-in % [:data :chrome/material :chrome/form]))
                 (:nodes slot))))))

(deftest handle-hit-full-zoom-envelope-with-scaled-container
  (let [selected (selection/marquee-commit
                  (selection/empty-selection)
                  #{{:vi :vi/fixture :address :fixture}})
        state (chrome/apply-selection (chrome/empty-state) selected frame nil nil)
        handle (first
                (filter #(= :handle
                            (get-in % [:data :chrome/material :chrome/form]))
                        (get-in state
                                [:slots [:chrome/target :vi/fixture :fixture]
                                 :nodes])))
        center (get-in handle [:data :chrome/hit-center-local])]
    (chrome/set-live-effective-provider! (constantly effective-a))
    (doseq [zoom [0.01 0.1 1.0 8.0 100.0 1000.0]]
      (chrome/set-live-camera-provider! (constantly {:zoom zoom}))
      (let [scale 2.0
            radius (/ (+ (/ 10.0 2.0) 6.0) (* zoom scale))]
        (is (chrome/handle-hit? handle (mapv + center [radius 0.0]))
            (str "body plus slop hits at zoom " zoom))
        (is (not (chrome/handle-hit?
                  handle (mapv + center [(+ radius (/ 0.001 zoom)) 0.0])))
            (str "outside body plus slop misses at zoom " zoom))))))
