(ns app.client.view.run-test
  "The first view through the executor on the JVM reference runner: each run
   is laid out and painted on its own surface, the pointer answers, a
   keystroke resumes the run it grew, a tool edit reruns, the store logs;
   with the box font for exact numbers and with Noto Sans from its file."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.walk]
            [app.client.engine.value-bytes :as vb]
            [app.client.text.truetype :as tt]
            [app.client.view.records :as records]
            [app.client.view.run :as run]
            [app.client.view.store :as store]
            [app.client.view.table :as table]))

;; Zoom 1 with the box font: local units are texels and every number is exact.
(def view (assoc (store/record records/store "view-1") :zoom 1 :pins {:font "font-boxes@1" :cursor "cursor-1"}))
(defn frame ([store] (run/frame store view)) ([store previous] (run/frame store view {:previous previous})))
(defn placement [f run ch] (first (filter #(= ch (:ch %)) (get-in f [:placements run]))))
(defn statuses [f] (into {} (for [[key s] (:statuses f)] [key (:status s)])))
(defn painting [f run] (:surface (first (filter #(= run (:run %)) (:paintings f)))))
(defn centre [p] (let [[x y w h] (:rect p)] [(+ x (/ w 2.0)) (+ y (/ h 2.0))]))
(defn nums
  "Every number as a double: the font scale is a ratio, so placements come
   back as doubles where the boxes were integers."
  [v]
  (clojure.walk/postwalk #(if (number? %) (double %) %) v))

(deftest each-run-draws-on-its-own-surface-and-the-pointer-answers
  (let [f (frame records/store)]
    (is (= ["run-1" "run-2"] (:order f)))
    (is (every? #(= :complete %) (vals (statuses f))) (pr-str (:statuses f)))
    (is (= 9 (count (:statuses f))) "one query, four per-run tools twice")
    (is (= [27 20] [(count (get-in f [:placements "run-1"])) (count (get-in f [:placements "run-2"]))]))
    (is (= (nums [{:ch "t" :rect [5 4 2 8] :pen [4 4]} {:ch "h" :rect [9 4 4 8] :pen [8 4]}])
           (nums (mapv #(select-keys % [:ch :rect :pen]) (take 2 (get-in f [:placements "run-1"]))))))
    (is (= [4.0 18.0] (nums (:pen (placement f "run-1" "f")))) "'fingers' wrapped whole to the second line; the space before it hangs")
    (is (= [[1 16] [0 12] [1 37]]
           (mapv (juxt :word-start :word-advance)
                 (let [items (table/shape records/run-1 records/font-boxes {})] [(nth items 0) (nth items 1) (first (filter #(= "f" (:ch %)) items))])))
        "the shaper carries each glyph's word start and its word's advance")
    (is (= [42.0 4.0] (nums (:caret f))) "offset 8 stands before the space after 'the tool'")
    (testing "a run's surface is its box: origin, wrap width, the lines it filled"
      (is (= {:x 4 :y 4 :w 102 :h 28} (:box (first (:paintings f)))))
      (is (= [102 28] [(:width (painting f "run-1")) (:height (painting f "run-1"))]))
      (is (= 24 (:revision (painting f "run-1"))) "a fresh surface at 0, 23 inked glyphs, one caret bar")
      (is (= {:x 4 :y 40 :w 102 :h 14} (:box (second (:paintings f)))))
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
    (is (not (contains? (:resumed f) ["layout@1" "run-1"])) "a letter inside the last word changed that word's measure on its earlier glyphs, so the layout reran")
    (is (contains? (:resumed f) ["paint@1" "run-1"]) "but the placed outlines before it are the same values, so the painter painted the one new glyph")
    (is (= 1 (count (get-in f [:runs ["paint@1" "run-1"] :history]))) "a frame's history is the rows that ran in it")
    (is (contains? (:resumed f) ["layout@1" "run-2"]) "the other run resumed with nothing to do")
    (is (not (contains? (:resumed f) ["caret-place@1" "run-1"])) "the cursor moved, so the caret place reran")
    (let [spaced (-> records/store (store/append-key "run-1" {:i 27 :ch " " :t 9}) (store/edit "cursor-1" assoc :offset 28))
          g (frame spaced before)]
      (is (contains? (:resumed g) ["layout@1" "run-1"]) "a space ends the word: a tail append, and the layout resumes with it alone")
      (is (= 1 (count (get-in g [:runs ["layout@1" "run-1"] :history])))))
    (is (vb/equal? (:data (painting f "run-1")) (:data (painting fresh "run-1"))) "resumed and fresh paint the same bytes")
    (is (= (:placements f) (:placements fresh)))
    (is (not= (:caret before) (:caret f)) "the caret follows the cursor to the end")
    (is (= 28 (count (table/fold-keys (:keys (store/record typed "run-1"))))))
    (is (= [{:op :put :id "run-1"} {:op :put :id "cursor-1"}]
           (mapv #(dissoc % :previous) (take-last 2 (:log typed)))))
    (is (= records/run-1 (:previous (nth (:log typed) 12))) "the store keeps what a put replaced"))
  (let [before (frame records/store)
        narrow (store/edit records/store "layout@1" assoc-in [:tool :width] 30)
        f (frame narrow before)
        wide (frame records/store)]
    (is (every? #(= :complete %) (vals (statuses f))) (pr-str (:statuses f)))
    (is (not (contains? (:resumed f) ["layout@1" "run-1"])) "an edited tool is a fresh run")
    (is (not= (:pen (placement wide "run-1" "u")) (:pen (placement f "run-1" "u"))) "the wrap moved")
    (is (= {:x 4 :y 4 :w 32 :h 84} (:box (first (:paintings f)))) "six lines at width 30: words kept whole, 'fingers' broken where no line holds it")
    (is (= [4.0 18.0] (nums (:pen (nth (get-in f [:placements "run-1"]) 4)))) "'the' fits line one; 'tool' opens line two whole")
    (is (= (store/record records/store "cursor-1") (store/record narrow "cursor-1")) "the cursor record is untouched")
    (is (= :text/run (:kind (store/record narrow "run-1"))))
    (is (= [23.0 18.0] (nums (:caret f))) "offset 8 now stands after 'tool' on the second line: the cursor did not move, its place did"))
  (let [before (frame records/store)
        deleted (-> records/store
                    (store/append-key "run-1" {:i 27 :key "Backspace" :t 9})
                    (store/edit "cursor-1" assoc :offset 26))
        f (frame deleted before)]
    (is (= 26 (count (get-in f [:placements "run-1"]))))
    (is (not (contains? (:resumed f) ["layout@1" "run-1"])) "a delete is not a tail append")
    (is (every? #(= :complete %) (vals (statuses f))) (pr-str (:statuses f)))))

(deftest noto-sans-from-its-file-draws-the-same-runs
  (let [bytes (.readAllBytes (io/input-stream (io/file "resources/public/fonts/noto_sans_regular.ttf")))
        noto (tt/parse bytes)
        table (table/with-fonts {"font-noto-sans@1" noto})
        store (store/put records/store (merge records/font-noto (tt/metrics noto) {:digest (tt/digest bytes)}))
        view (assoc (store/record store "view-1") :zoom 1)
        f (run/frame store view {:table table})
        first-t (first (get-in f [:placements "run-1"]))]
    (is (every? #(= :complete %) (vals (statuses f))) (pr-str (:statuses f)))
    (is (= [27 20] [(count (get-in f [:placements "run-1"])) (count (get-in f [:placements "run-2"]))]))
    (is (= "t" (:ch first-t)))
    (is (< 0 (nth (:rect first-t) 2) 10) "a real advance and box at size 10")
    (is (<= 28 (:h (:box (first (:paintings f))))) "the sentence wraps at width 100")
    (is (= 24 (:revision (:surface (first (:paintings f))))) "23 inked glyphs and the caret, as with the boxes")
    (is (= {:what :text/run :run "run-1" :glyph "t" :index 0 :by "sid" :drawn-by "paint@1"}
           (dissoc (run/hit store view f (centre first-t) {:table table}) :where)))
    (is (= "a" (:glyph (run/hit store view f (centre (first (get-in f [:placements "run-2"]))) {:table table}))))
    (is (nil? (run/hit store view f [200 200] {:table table})))
    (is (pos? (:changed (:surface (first (:paintings f))))) "the caret bar left texels behind it")
    (is (= :font/unloaded (get-in (run/frame store view) [:runs ["layout@1" "run-1"] :data :error-type]))
        "without the file's table the layout says so")))

(deftest the-store-is-records-by-id-in-order
  (is (= ["caret-paint@1" "caret-place@1" "cursor-1" "font-boxes@1" "font-noto-sans@1" "hit@1" "layout@1" "paint@1" "query@1" "run-1" "run-2" "view-1"]
         (mapv :id (store/records records/store))))
  (is (every? #(and (string? (:id %)) (keyword? (:kind %)) (string? (:by %))) (store/records records/store))))
