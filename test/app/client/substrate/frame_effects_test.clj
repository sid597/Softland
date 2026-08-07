(ns app.client.substrate.frame-effects-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.substrate.frame-effects :as effects]
            [app.client.workspace.containers :as containers]))

(defn- entry [id path pass-class]
  {:entry/id id :order {:stratum :world :pass-class pass-class
                        :stack-path path :part-rank 0 :stable-tie id}})

(defn- registry []
  (-> (containers/empty-registry)
      (containers/add-container :group
                                {:parent nil :layer 1 :sibling-rank 1
                                 :effects {:opacity 0.5}})
      (containers/add-container :inner
                                {:parent :group :layer 2 :sibling-rank 2
                                 :effects {:isolate? true
                                           :layer-blur
                                           {:radius-world 12.0 :max-px 64.0
                                            :algorithm-version
                                            effects/blur-algorithm-version}}})))

(deftest effect-grammar-derived-contract-and-radius-projection
  (let [normalized (effects/validate-effects!
                    {:opacity 0.5 :mask :mask-child
                     :backdrop-blur {:radius-world 40.0 :max-px 64.0
                                     :algorithm-version
                                     effects/blur-algorithm-version}})
        declarations (effects/derived-effect-declarations normalized)
        projection (effects/projected-blur (:backdrop-blur normalized) 2.0 2.0)
        z0p1 (effects/projected-blur (:backdrop-blur normalized) 0.45 0.1)
        z1 (effects/projected-blur (:backdrop-blur normalized) 0.45 1.0)]
    (is (= 0.5 (:opacity normalized)))
    (is (= #{:alpha-mask :backdrop-blur :group-composite}
           (set (map :kind declarations))))
    (is (every? #(= :none (:pick %)) declarations))
    (is (= {:radius-px 64.0 :raw-radius-px 160.0 :clamped? true
            :regime :max-px-clamped
            :algorithm-version effects/blur-algorithm-version
            :downsample-levels 3}
           projection))
    (is (< (Math/abs (- 1.8 (:radius-px z0p1))) 1.0e-9))
    (is (= 18.0 (:radius-px z1)))
    (is (< (:radius-px z0p1) (:radius-px z1)))
    (doseq [bad [{:opacity 1.1}
                 {:blend-mode :multiply}
                 {:mask nil}
                 {:layer-blur {:radius-world -1
                               :algorithm-version effects/blur-algorithm-version}}
                 {:layer-blur {:radius-world 1 :algorithm-version :unversioned}}]]
      (is (thrown? clojure.lang.ExceptionInfo
                   (effects/validate-effects! bad))))))

(deftest split-spans-nest-inner-first-and-maintain-proportionally
  (let [reg (registry)
        paths (containers/effective reg)
        group-path (:stack-path (get paths :group))
        inner-path (:stack-path (get paths :inner))
        arrangement [(entry :group-a group-path :direct)
                     (entry :inner inner-path :direct)
                     (entry :outside [[:outside 9 9]] :direct)
                     ;; pass-class precedes stack-path, so one semantic group
                     ;; can bind a second disjoint range.
                     (entry :group-b group-path :intermediate)]
        spans (effects/derive-effect-spans reg arrangement)
        by-id (into {} (map (juxt :container/id identity)) spans)
        state-1 (effects/maintain-effect-spans nil reg arrangement)
        sibling-span (get-in state-1 [:spans-by-container :inner])
        reg-2 (assoc-in reg [:containers :group :effects :opacity] 0.4)
        state-2 (effects/maintain-effect-spans state-1 reg-2 arrangement)
        state-3 (effects/maintain-effect-spans state-2 reg-2 arrangement)]
    (is (= [:inner :group] (mapv :container/id spans)))
    (is (= [[0 2] [3 4]] (get-in by-id [:group :entry-ranges])))
    (is (= :group (get-in by-id [:inner :parent/container-id])))
    (is (= [:group :inner]
           (mapv :container/id (effects/entry-effect-chain spans 1))))
    (is (= {:containers 1 :siblings 0 :full? false}
           (:last-derivation state-2)))
    (is (= {:containers 0 :siblings 0 :full? false}
           (:last-derivation state-3)))
    (is (identical? sibling-span (get-in state-2 [:spans-by-container :inner])))
    (is (= spans (effects/derive-effect-spans reg arrangement)))))

(deftest group-opacity-is-applied-once-after-sibling-composition
  (let [left [0.5 0.0 0.0 0.5]
        right [0.0 0.0 0.5 0.5]
        correct (effects/composite-group [left right]
                                         {:opacity 0.5 :mask-alpha 1.0})
        wrong (effects/source-over
               (effects/apply-group-opacity right 0.5)
               (effects/apply-group-opacity left 0.5))
        deltas (mapv #(Math/abs (- %1 %2)) correct wrong)
        epsilon (/ 1.0 255.0)]
    (is (= [0.125 0.0 0.25 0.375] correct))
    (is (> (apply max deltas) (* 10.0 epsilon)))
    (is (= [0.0625 0.0 0.125 0.1875]
           (effects/apply-alpha-mask correct 0.5)))))
