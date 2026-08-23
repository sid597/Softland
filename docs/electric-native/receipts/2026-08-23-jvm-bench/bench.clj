(ns bench
  (:require [app.client.workspace.scene-store :as ss]
            [app.client.workspace.containers :as ctn]
            [app.client.workspace.rect-tree :as rt]))

;; ---------------------------------------------------------------------------
;; Fixture: one container at identity; N slots stacked vertically, no overlap.
;; Each slot = block-shaped rect tree: 1 container rect + 6 text-line children,
;; every child carrying a distinct :address + one text op.
;; ---------------------------------------------------------------------------

(def SLOT-H 100.0)
(def SLOT-PITCH 120.0)
(def LINES 6)

(defn block-tree [i tag]
  (rt/rt-node (keyword (str "block-" i)) :box
              {:x 0 :y (* i SLOT-PITCH) :w 300 :h SLOT-H}
              :style {:bg [0.16 0.17 0.22 1.0] :radius 6 :border-width 1 :border-color [0.3 0.3 0.4 1.0]}
              :data {:address (str "block-" i)}
              :children
              (mapv (fn [j]
                      (rt/rt-node (keyword (str "line-" i "-" j)) :box
                                  {:x 8 :y (+ 8.0 (* j 14.0)) :w 284 :h 12}
                                  :style {:bg [0.22 0.23 0.30 1.0]}
                                  :data {:address (str "block-" i "/line-" j)}
                                  :text [{:text (str tag " line " j " of block " i)
                                          :type :text :x 4 :y 2 :size 14}]))
                    (range LINES))))

(defn vi-of [i] [:vi :bench i])

(def registry (-> (ctn/empty-registry)
                  (ctn/add-container 1 {:x 0 :y 0 :scale 1 :layer 0})))
(def effective (ctn/effective registry))

(defn build-store [n]
  (reduce (fn [st i]
            (ss/upsert-slot st (vi-of i) {:tree (block-tree i "a") :container 1}))
          (ss/empty-store)
          (range n)))

;; probe points (world == container-local, identity affine)
(defn line-point [i j] [150.0 (+ (* i SLOT-PITCH) 8.0 (* j 14.0) 6.0)])

;; ---------------------------------------------------------------------------
;; Timing
;; ---------------------------------------------------------------------------

(defn median [xs] (let [v (vec (sort xs)) c (count v)]
                    (if (odd? c) (nth v (quot c 2))
                        (/ (+ (nth v (dec (quot c 2))) (nth v (quot c 2))) 2.0))))

(defn bench-ns
  "Median nanoseconds of (f) over `reps` timed runs after `warm` warmups."
  ([f] (bench-ns f 10 41))
  ([f warm reps]
   (dotimes [_ warm] (f))
   (median (repeatedly reps (fn [] (let [t0 (System/nanoTime)
                                         r  (f)
                                         t1 (System/nanoTime)]
                                     (when (nil? r) nil) ; keep result alive
                                     (- t1 t0)))))))

(defn row [n]
  (let [;; --- build total (single measured pass, after one warm pass) ---
        _        (build-store (min n 50))
        t0       (System/nanoTime)
        store    (build-store n)
        t1       (System/nanoTime)
        build-ms (/ (- t1 t0) 1e6)

        ;; --- last single upsert at size N (store of N-1, insert the Nth) ---
        store-1  (build-store (dec n))
        last-vi  (vi-of (dec n))
        last-tr  (block-tree (dec n) "a")
        last-up  (bench-ns #(ss/upsert-slot store-1 last-vi
                                            {:tree last-tr :container 1}))

        ;; --- picks ---
        p-last   (line-point (dec n) 3)
        p-first  (line-point 0 3)
        p-miss   [5000.0 5000.0]
        hit-last (ss/pick store effective p-last)
        hit-1st  (ss/pick store effective p-first)
        hit-miss (ss/pick store effective p-miss)
        t-last   (bench-ns #(ss/pick store effective p-last))
        t-first  (bench-ns #(ss/pick store effective p-first))
        t-miss   (bench-ns #(ss/pick store effective p-miss))

        ;; --- pick decomposition: entries materialization vs one hit-test ---
        t-entries (bench-ns #(count (ss/maintained-entries store)))
        one-tree (:tree (ss/slot store last-vi))
        t-hit1   (bench-ns #(rt/hit-test one-tree (first p-last) (second p-last)))

        ;; --- derive-store-frame over the whole store ---
        frame    (ss/derive-store-frame store)
        t-derive (bench-ns #(ss/derive-store-frame store) 5 20)

        ;; --- one edit: upsert an EXISTING key with a changed tree ---
        edit-i   (quot n 2)
        edit-vi  (vi-of edit-i)
        edit-tr  (block-tree edit-i "EDITED")
        t-edit   (bench-ns #(ss/upsert-slot store edit-vi
                                            {:tree edit-tr :container 1}))]
    (println (format "%6d | %12.2f | %13.1f | %11.1f | %12.1f | %11.1f | %16.3f | %13.1f | %13.1f | %12.2f"
                     n build-ms (/ last-up 1e3) (/ t-last 1e3) (/ t-first 1e3)
                     (/ t-miss 1e3) (/ t-derive 1e6) (/ t-edit 1e3)
                     (/ t-entries 1e3) (/ t-hit1 1e3)))
    (flush)
    {:n n :build-ms build-ms :last-upsert-us (/ last-up 1e3)
     :pick-last-us (/ t-last 1e3) :pick-first-us (/ t-first 1e3)
     :pick-miss-us (/ t-miss 1e3) :derive-ms (/ t-derive 1e6)
     :edit-upsert-us (/ t-edit 1e3)
     :entries-us (/ t-entries 1e3) :one-hittest-us (/ t-hit1 1e3)
     :sanity {:hit-last (:address hit-last)
              :hit-first (:address hit-1st)
              :hit-miss hit-miss
              :rects (count (:rects frame))
              :text-vis (count (:text-by-vi frame))
              :ordered (count (:ordered-vis frame))}}))

(defn -main []
  (println "N-slots | build-total-ms | last-upsert-us | pick-last-us | pick-first-us | pick-miss-us | derive-store-frame-ms | edit-upsert-us | [entries]-us | [1 hit-test]-us")
  (println "--------|----------------|----------------|--------------|---------------|--------------|-----------------------|----------------|--------------|----------------")
  (let [rows (mapv row [10 200 2000])]
    (println)
    (println "SANITY:")
    (doseq [r rows] (println (format "  N=%-5d %s" (:n r) (pr-str (:sanity r)))))
    (println)
    (println "RATIO 200 -> 2000 (10x slots; ~10 = linear, ~1 = constant, ~100 = quadratic):")
    (let [a (nth rows 1) b (nth rows 2)]
      (doseq [k [:build-ms :last-upsert-us :pick-last-us :pick-first-us
                 :pick-miss-us :derive-ms :edit-upsert-us :entries-us :one-hittest-us]]
        (println (format "  %-16s %.2fx" (name k) (double (/ (get b k) (get a k)))))))
    (println)
    (println "RATIO 10 -> 200 (20x slots):")
    (let [a (nth rows 0) b (nth rows 1)]
      (doseq [k [:build-ms :last-upsert-us :pick-last-us :pick-first-us
                 :pick-miss-us :derive-ms :edit-upsert-us :entries-us :one-hittest-us]]
        (println (format "  %-16s %.2fx" (name k) (double (/ (get b k) (get a k)))))))))

(-main)
