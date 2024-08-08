(ns app.client.quad-tree
  (:require [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            #?@(:cljs [[global-flow :refer [!node-pos-atom  node-pos-flow !global-atom global-client-flow current-time-ms debounce]]]
                :clj [[missionary.core :as m]
                      [com.rpl.rama.path :as path :refer [subselect ALL FIRST keypath select]]
                      [app.server.rama :as rama :refer [!subscribe get-path-data nodes-pstate update-node get-event-id node-ids-pstate]]])))

(defrecord QuadTree [min-x min-y width height nodes nw ne sw se])

(defn within-bounds? [node min-x min-y width height]
  (let [x (get-in node [:x :pos])
        y (get-in node [:y :pos])]
    (and (>= x min-x) (< x (+ min-x width))
      (>= y min-y) (< y (+ min-y height)))))

(defn split-quad [nodes min-x min-y width height]
  (let [hw (/ width 2)
        hh (/ height 2)
        mid-x (+ min-x hw)
        mid-y (+ min-y hh)]
    {:nw (filterv #(within-bounds? % min-x min-y hw hh) nodes)
     :ne (filterv #(within-bounds? % mid-x min-y hw hh) nodes)
     :sw (filterv #(within-bounds? % min-x mid-y hw hh) nodes)
     :se (filterv #(within-bounds? % mid-x mid-y hw hh) nodes)}))

(defn build-quad-tree [nodes min-x min-y width height]
  ;(println "Build quad tree")
  (let [max-nodes 4
        nodes (filterv #(within-bounds? % min-x min-y width height) nodes)]
    (if (<= (count nodes) max-nodes)
      (->QuadTree min-x min-y width height nodes nil nil nil nil)
      (let [{:keys [nw ne sw se]} (split-quad nodes min-x min-y width height)]
        (->QuadTree min-x min-y width height nil
          (build-quad-tree nw min-x min-y (/ width 2) (/ height 2))
          (build-quad-tree ne (+ min-x (/ width 2)) min-y (/ width 2) (/ height 2))
          (build-quad-tree sw min-x (+ min-y (/ height 2)) (/ width 2) (/ height 2))
          (build-quad-tree se (+ min-x (/ width 2)) (+ min-y (/ height 2)) (/ width 2) (/ height 2)))))))

(defn distance-squared [x1 y1 x2 y2]
  (+ (Math/pow (- x2 x1) 2) (Math/pow (- y2 y1) 2)))

(defonce G 10.0)
(defonce m 100.0)

(defn calculate-force-components [node1 node2]
  (let [x1 (get-in node1 [:x :pos])
        y1 (get-in node1 [:y :pos])
        x2 (get-in node2 [:x :pos])
        y2 (get-in node2 [:y :pos])
        epsilon 0.001  ;; Small value to prevent division by zero
        dx (- x2 x1)
        dy (- y2 y1)
        r-sq (+ (distance-squared x1 y1 x2 y2) epsilon)
        force (/ (* G m m) r-sq)
        r (Math/sqrt r-sq)]
    ;(println "FFF" force "::" r-sq "::" x1 y1 x2 y2)
    {:fx (* force (/ dx r))
     :fy (* force (/ dy r))}))

(defn center-of-mass [nodes]
  (let [total-mass (count nodes)
        sum-x (reduce + (map #(get-in % [:x :pos]) nodes))
        sum-y (reduce + (map #(get-in % [:y :pos]) nodes))]
    {:x (/ sum-x total-mass)
     :y (/ sum-y total-mass)}))

(defn sum-forces [forces]
  (let [res (reduce
              (fn [acc force]
                (-> acc
                  (update :fx + (:fx force))
                  (update :fy + (:fy force))))
              {:fx 0.0 :fy 0.0}
              forces)]
    ;(println "FORCES" forces "::" res)
    res))


(defn approximate-force [node quad-tree theta]
  (if (and (nil? (:nw quad-tree)) (nil? (:ne quad-tree)) (nil? (:sw quad-tree)) (nil? (:se quad-tree)))
    ;; If the current node is a leaf
    (sum-forces (map #(calculate-force-components node %) (:nodes quad-tree)))
    ;; If the current node is an internal node
    (let [x1 (get-in node [:x :pos])
          y1 (get-in node [:y :pos])
          {:keys [x y]} (center-of-mass (:nodes quad-tree))
          dx (- x x1)
          dy (- y y1)
          r-sq (+ (distance-squared x1 y1 x y) 0.001)
          r (Math/sqrt r-sq)
          s (:width quad-tree)]
      (if (< s (/ r theta))
        ;; If the current node can be treated as a single body
        (let [force  (/ (* G m m) r-sq)
              fx (* force (/ dx r))
              fy (* force (/ dy r))]
          {:fx fx :fy fy})
        ;; Otherwise, recurse into the children
        (sum-forces (map #(approximate-force node % theta)
                      [(:nw quad-tree) (:ne quad-tree) (:sw quad-tree) (:se quad-tree)]))))))

;; WORK IN PROGRESS

(e/defn total-forces [min-x min-y max-x max-y]
  #_(e/client
      (doseq [tick [1 2]]
        (println "TICK" tick)))
  (e/server
    (let [nodes (into [] (vals (first (get-path-data [(keypath :main)] nodes-pstate))))]
      (e/client
        (let [quad-tree (build-quad-tree nodes
                          (- @min-x 900)
                          (- @min-x 900)
                          (+ (- @max-x @min-x) 1900)
                          (+ (- @max-y @min-y) 1900))
              theta 0.5
              res (atom {})]
          (do
            (doseq [node nodes]
              (let [{:keys [fx fy]} (approximate-force node quad-tree theta)]
                (do
                 (println "NODES" (:id node) fx fy)
                 (swap! res assoc (:id node) {:fx fx :fy fy}))))
            (println "RR" @res))
          #_(doseq [node @res]
              (reset! !global-atom {:type :tick
                                    :nid (:id node)
                                    :time (current-time-ms)
                                    :fx fx
                                    :fy fy}))
          @res)))))
