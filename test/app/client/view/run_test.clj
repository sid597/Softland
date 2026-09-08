(ns app.client.view.run-test
  "The first view through the executor on the JVM reference runner: it draws,
   the pointer answers, a keystroke and a tool edit take hold, the store logs."
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.view.records :as records]
            [app.client.view.run :as run]
            [app.client.view.store :as store]
            [app.client.view.table :as table]))

;; Zoom 1 keeps the JVM runner's reflective float loops short; local units are texels.
(def view (assoc (store/record records/store "view-1") :zoom 1))
(def size [120 68])
(defn frame [store] (run/frame store view size))
(defn placement [f ch] (first (filter #(= ch (:ch %)) (get-in f [:placements :items]))))
(defn hit-at [store f point] (get-in (run/hit store view f point) [:results :hit]))

(deftest the-view-draws-and-the-pointer-answers
  (let [f (frame records/store)]
    (is (= {"query@1" :complete "layout@1" :complete "paint@1" :complete}
           (into {} (for [[id s] (:statuses f)] [id (:status s)]))))
    (is (= 47 (count (get-in f [:placements :items]))) "27 keys of run-1 and 20 of run-2")
    (is (= [{:ch "t" :rect [5 4 2 8] :pen [4 4]} {:ch "h" :rect [9 4 4 8] :pen [8 4]}]
           (mapv #(select-keys % [:ch :rect :pen]) (take 2 (get-in f [:placements :items])))))
    (is (= [42 4] (:caret f)) "offset 8 stands before the space after 'the tool'")
    (is (= 42 (:revision (:painting f))) "one fresh surface, 40 inked glyphs, one caret bar")
    (is (pos? (get-in f [:runs "layout@1" :results :outline :rings 0 :anchors 0 :p 0])))
    (testing "point at the text: what, where, whose, drawn by which tool"
      (is (= {:what :text/run :run "run-1" :glyph "t" :index 0 :where [4 4] :by "sid" :drawn-by "paint@1"}
             (hit-at records/store f [5.5 6.5])))
      (is (= {:what :text/run :run "run-2" :glyph "a" :index 0 :where [4 40] :by "clipboard" :drawn-by "paint@1"}
             (hit-at records/store f [5 44])))
      (is (nil? (hit-at records/store f [50 60]))))
    (testing "a foreign run draws at the tool's foreign scale: 'p' after 'a' and a half-width space"
      (is (= [9.0 41.5 2.0 3.5] (:rect (placement f "p")))))))

(deftest a-keystroke-and-a-tool-edit-take-hold
  (let [before (frame records/store)
        typed (-> records/store
                  (store/append-key "run-1" {:i 27 :ch "!" :t 9})
                  (store/edit "cursor-1" assoc :offset 28))
        f (frame typed)]
    (is (= 48 (count (get-in f [:placements :items]))))
    (is (not= (:caret before) (:caret f)) "the caret follows the cursor to the end")
    (is (= 28 (count (table/fold-keys (:keys (store/record typed "run-1"))))))
    (is (= [{:op :put :id "run-1"} {:op :put :id "cursor-1"}]
           (mapv #(dissoc % :previous) (take-last 2 (:log typed)))))
    (is (= records/run-1 (:previous (nth (:log typed) 9))) "the store keeps what a put replaced"))
  (let [narrow (store/edit records/store "layout@1" assoc-in [:tool :width] 30)
        f (frame narrow)
        wide (frame records/store)]
    (is (= :complete (get-in f [:statuses "paint@1" :status])))
    (is (not= (:pen (placement wide "u")) (:pen (placement f "u"))) "the wrap moved")
    (is (= (store/record records/store "cursor-1") (store/record narrow "cursor-1")) "the cursor record is untouched")
    (is (= :text/run (:kind (store/record narrow "run-1"))))
    (is (= [13 14] (:caret f)) "offset 8 now stands on the second line: the cursor did not move, its place did")))

(deftest the-store-is-records-by-id-in-order
  (is (= ["cursor-1" "font-boxes@1" "hit@1" "layout@1" "paint@1" "query@1" "run-1" "run-2" "view-1"]
         (mapv :id (store/records records/store))))
  (is (every? #(and (string? (:id %)) (keyword? (:kind %)) (string? (:by %))) (store/records records/store))))
