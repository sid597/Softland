(ns app.client.view.run-test
  "The first view through the executor on the JVM reference runner: each run
   is laid out and painted on its own surface, the pointer answers, a
   keystroke resumes the run it grew, a tool edit reruns, the store logs."
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.engine.value-bytes :as vb]
            [app.client.view.records :as records]
            [app.client.view.run :as run]
            [app.client.view.store :as store]
            [app.client.view.table :as table]))

;; Zoom 1: local units are texels, and the reference runner's loops stay short.
(def view (assoc (store/record records/store "view-1") :zoom 1))
(defn frame ([store] (run/frame store view)) ([store previous] (run/frame store view {:previous previous})))
(defn placement [f run ch] (first (filter #(= ch (:ch %)) (get-in f [:placements run]))))
(defn statuses [f] (into {} (for [[key s] (:statuses f)] [key (:status s)])))
(defn painting [f run] (:surface (first (filter #(= run (:run %)) (:paintings f)))))

(deftest each-run-draws-on-its-own-surface-and-the-pointer-answers
  (let [f (frame records/store)]
    (is (= ["run-1" "run-2"] (:order f)))
    (is (every? #(= :complete %) (vals (statuses f))) (pr-str (:statuses f)))
    (is (= 9 (count (:statuses f))) "one query, four per-run tools twice")
    (is (= [27 20] [(count (get-in f [:placements "run-1"])) (count (get-in f [:placements "run-2"]))]))
    (is (= [{:ch "t" :rect [5 4 2 8] :pen [4 4]} {:ch "h" :rect [9 4 4 8] :pen [8 4]}]
           (mapv #(select-keys % [:ch :rect :pen]) (take 2 (get-in f [:placements "run-1"])))))
    (is (= [4 14] (:pen (placement f "run-1" "f"))) "'fingers' wrapped to the second line")
    (is (= [42 4] (:caret f)) "offset 8 stands before the space after 'the tool'")
    (testing "a run's surface is its box: origin, wrap width, the lines it filled"
      (is (= {:x 4 :y 4 :w 102 :h 20} (:box (first (:paintings f)))))
      (is (= [102 20] [(:width (painting f "run-1")) (:height (painting f "run-1"))]))
      (is (= 24 (:revision (painting f "run-1"))) "a fresh surface at 0, 23 inked glyphs, one caret bar")
      (is (= {:x 4 :y 40 :w 102 :h 10} (:box (second (:paintings f)))))
      (is (= 18 (:revision (painting f "run-2"))) "17 inked glyphs and an empty caret paint"))
    (testing "point at the text: what, where, whose, drawn by which tool"
      (is (= {:what :text/run :run "run-1" :glyph "t" :index 0 :where [4 4] :by "sid" :drawn-by "paint@1"}
             (run/hit records/store view f [5.5 6.5])))
      (is (= {:what :text/run :run "run-2" :glyph "a" :index 0 :where [4 40] :by "clipboard" :drawn-by "paint@1"}
             (run/hit records/store view f [5 44])))
      (is (nil? (run/hit records/store view f [50 60]))))
    (testing "a foreign run draws at the tool's foreign scale: 'p' after 'a' and a half-width space"
      (is (= [9.0 41.5 2.0 3.5] (:rect (placement f "run-2" "p")))))))

(deftest a-keystroke-resumes-the-run-it-grew-and-edits-rerun
  (let [before (frame records/store)
        typed (-> records/store
                  (store/append-key "run-1" {:i 27 :ch "!" :t 9})
                  (store/edit "cursor-1" assoc :offset 28))
        f (frame typed before)
        fresh (frame typed)]
    (is (every? #(= :complete %) (vals (statuses f))) (pr-str (:statuses f)))
    (is (= 28 (count (get-in f [:placements "run-1"]))))
    (is (contains? (:resumed f) ["layout@1" "run-1"]) "the grown run's layout resumed")
    (is (contains? (:resumed f) ["paint@1" "run-1"]) "and its painter painted the one new glyph")
    (is (contains? (:resumed f) ["layout@1" "run-2"]) "the other run resumed with nothing to do")
    (is (not (contains? (:resumed f) ["caret-place@1" "run-1"])) "the cursor moved, so the caret place reran")
    (is (= 1 (count (get-in f [:runs ["layout@1" "run-1"] :history]))) "a frame's history is the rows that ran in it")
    (is (vb/equal? (:data (painting f "run-1")) (:data (painting fresh "run-1"))) "resumed and fresh paint the same bytes")
    (is (= (:placements f) (:placements fresh)))
    (is (not= (:caret before) (:caret f)) "the caret follows the cursor to the end")
    (is (= 28 (count (table/fold-keys (:keys (store/record typed "run-1"))))))
    (is (= [{:op :put :id "run-1"} {:op :put :id "cursor-1"}]
           (mapv #(dissoc % :previous) (take-last 2 (:log typed)))))
    (is (= records/run-1 (:previous (nth (:log typed) 11))) "the store keeps what a put replaced"))
  (let [before (frame records/store)
        narrow (store/edit records/store "layout@1" assoc-in [:tool :width] 30)
        f (frame narrow before)
        wide (frame records/store)]
    (is (every? #(= :complete %) (vals (statuses f))) (pr-str (:statuses f)))
    (is (not (contains? (:resumed f) ["layout@1" "run-1"])) "an edited tool is a fresh run")
    (is (not= (:pen (placement wide "run-1" "u")) (:pen (placement f "run-1" "u"))) "the wrap moved")
    (is (= {:x 4 :y 4 :w 32 :h 50} (:box (first (:paintings f)))) "five lines at width 30")
    (is (= (store/record records/store "cursor-1") (store/record narrow "cursor-1")) "the cursor record is untouched")
    (is (= :text/run (:kind (store/record narrow "run-1"))))
    (is (= [13 14] (:caret f)) "offset 8 now stands on the second line: the cursor did not move, its place did"))
  (let [before (frame records/store)
        deleted (-> records/store
                    (store/append-key "run-1" {:i 27 :key "Backspace" :t 9})
                    (store/edit "cursor-1" assoc :offset 26))
        f (frame deleted before)]
    (is (= 26 (count (get-in f [:placements "run-1"]))))
    (is (not (contains? (:resumed f) ["layout@1" "run-1"])) "a delete is not a tail append")
    (is (every? #(= :complete %) (vals (statuses f))) (pr-str (:statuses f)))))

(deftest the-store-is-records-by-id-in-order
  (is (= ["caret-paint@1" "caret-place@1" "cursor-1" "font-boxes@1" "hit@1" "layout@1" "paint@1" "query@1" "run-1" "run-2" "view-1"]
         (mapv :id (store/records records/store))))
  (is (every? #(and (string? (:id %)) (keyword? (:kind %)) (string? (:by %))) (store/records records/store))))
