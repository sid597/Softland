;; Ground check, part 1: a throwaway run script, not code. Run from a detached
;; checkout of HEAD that holds the run-again fixture of 0d24b03e (the second making
;; of 14 October 2026), so that the fixture read as a resource is the old one:
;;
;;   clojure -M:store-module <this file>
;;
;; It does what check 2 of softland.store.run-again-proof does up to the
;; comparison (launch `Store` with 4 tasks on an in-process cluster, append the
;; fixture's records by hand as they are, with their own times, in the order
;; :as-written, wait for rest, read every table on every task, bring them into the
;; form with no task) using that namespace's own functions, and then prints the
;; whole comparison against the fixture's tables.edn, every table of both gates,
;; every row that differs. It writes no file and changes nothing.
(ns ground-1-compare
  (:require [softland.store.run-again-proof]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def fixture-read #'softland.store.run-again-proof/fixture-read)
(def tables-of-the-file #'softland.store.run-again-proof/tables-of-the-file)
(def on-a-store #'softland.store.run-again-proof/on-a-store)
(def feeds! #'softland.store.run-again-proof/feeds-the-fixtures-records!)
(def first-at-rest! #'softland.store.run-again-proof/first-at-rest!)
(def tables-held #'softland.store.run-again-proof/tables-held)
(def task-free #'softland.store.run-again-proof/task-free)
(def hand-var #'softland.store.run-again-proof/*hand*)
(def gate-order @#'softland.store.run-again-proof/gate-order)
(def table-order @#'softland.store.run-again-proof/table-order)

(defn cut [x n] (let [s (if (string? x) x (pr-str x))] (if (> (count s) n) (str (subs s 0 n) "...") s)))

(defn norm-key
  "A key with each token that is all digits put as #: a key that holds a stamp
  (the facts by value) pairs with the same key holding another stamp."
  [k]
  (if (string? k) (str/join " " (map (fn [t] (if (re-matches #"\d+" t) "#" t)) (str/split k #" "))) k))

(defn fields-apart [a b]
  (if (and (map? a) (map? b))
    (vec (sort (filter (fn [f] (not= (get a f) (get b f))) (distinct (concat (keys a) (keys b))))))
    [:the-value]))

(defn part-word
  "What differs, in words: stamp, time, or the other field's name."
  [a b]
  (let [fs (fields-apart a b)]
    (mapv (fn [f] (case f :stamp "stamp" :time (cond (and (map? a) (not (contains? a :time))) "time (only the store holds one)"
                                                     (and (map? b) (not (contains? b :time))) "time (only the fixture holds one)"
                                                     :else "time (both hold one, other values)")
                        :the-value "the value" (name f)))
          fs)))

(defn compare-table
  [gate table exp got]
  (let [ks (distinct (concat (keys exp) (keys got)))
        both (filter (fn [k] (and (contains? exp k) (contains? got k))) ks)
        equal (filter (fn [k] (= (get exp k) (get got k))) both)
        in-place (remove (fn [k] (= (get exp k) (get got k))) both)
        f-only (remove (fn [k] (contains? got k)) (keys exp))
        s-only (remove (fn [k] (contains? exp k)) (keys got))
        f-by-norm (group-by norm-key f-only)
        s-by-norm (group-by norm-key s-only)
        pairs (vec (for [[nk fks] f-by-norm
                         :let [sks (get s-by-norm nk)]
                         :when (and (= 1 (count fks)) (= 1 (count sks)) (not= nk (first fks)))]
                     [(first fks) (first sks)]))
        paired-f (set (map first pairs))
        paired-s (set (map second pairs))]
    {:gate gate :table table
     :fixture-rows (count exp) :store-rows (count got)
     :equal (count equal) :in-place (vec (sort-by pr-str in-place))
     :pairs (vec (sort-by pr-str pairs))
     :fixture-only (vec (sort-by pr-str (remove paired-f f-only)))
     :store-only (vec (sort-by pr-str (remove paired-s s-only)))}))

(defn report! [{:keys [gate table fixture-rows store-rows equal in-place pairs fixture-only store-only]} exp got]
  (println "TABLE" (name gate) (name table) "| fixture rows:" fixture-rows "| store rows:" store-rows
           "| equal:" equal "| same key, other value:" (count in-place)
           "| key holds a stamp, paired with the store's key that holds another:" (count pairs)
           "| fixture only, unpaired:" (count fixture-only) "| store only, unpaired:" (count store-only))
  (println "  CLASSES same key, other value, by what differs:"
           (pr-str (into (sorted-map) (frequencies (map (fn [k] (part-word (get exp k) (get got k))) in-place)))))
  (println "  CLASSES key holds a stamp, by what differs in the paired rows:"
           (pr-str (into (sorted-map) (frequencies (map (fn [[fk sk]] (part-word (get exp fk) (get got sk))) pairs)))))
  (doseq [k in-place
          :let [a (get exp k) b (get got k) pw (part-word a b)
                only-stamp-time? (every? (fn [w] (or (= "stamp" w) (str/starts-with? w "time"))) pw)]]
    (println "ROW" (name gate) (name table) "key" (cut k 160) "| differs:" (pr-str pw)
             (if only-stamp-time?
               (str "| fixture stamp " (if (map? a) (:stamp a) a) " -> store stamp " (if (map? b) (:stamp b) b)
                    (when (and (map? b) (contains? b :time)) (str ", store time " (:time b))))
               (str "| fixture: " (cut a 500) " | store: " (cut b 500)))))
  (doseq [[fk sk] pairs]
    (println "PAIR" (name gate) (name table) "fixture key" (cut fk 160) "| store key" (cut sk 160)
             "| rows differ in:" (pr-str (part-word (get exp fk) (get got sk)))))
  (doseq [k fixture-only]
    (println "FIXTURE ONLY" (name gate) (name table) "key" (cut k 200) "| row:" (cut (get exp k) 500)))
  (doseq [k store-only]
    (println "STORE ONLY" (name gate) (name table) "key" (cut k 200) "| row:" (cut (get got k) 500))))

(defn -main []
  (let [{:keys [acts] file :tables} (fixture-read)
        expected (tables-of-the-file file)]
    (println "FIXTURE read from" (str (io/resource "softland/store/run_again_fixture/tables.edn")))
    (println "FIXTURE acts head:" (pr-str (select-keys (:head acts) [:made :tasks :records])))
    (println "FIXTURE tables head rows:" (pr-str (get-in file [:head :rows])))
    (on-a-store
     4
     (fn []
       (let [began (System/currentTimeMillis)
             _ (feeds! acts :as-written)
             rested (first-at-rest!)
             hand @hand-var
             held (tables-held (:first hand))
             {:keys [faults] here :tables} (task-free held)
             results (vec (for [gate gate-order table table-order]
                            (compare-table gate table (get-in expected [gate table] {}) (get-in here [gate table] {}))))]
         (println "FED records appended by hand, order :as-written, 4 tasks | not acked:" (count @(:not-appended hand))
                  "| at rest:" (pr-str rested) "| feeding and wait:" (- (System/currentTimeMillis) began) "ms")
         (println "FAULTS of the form with no task (a task with another copy, or a key on two tasks):" (count faults)
                  (cut (vec (take 5 faults)) 800))
         (doseq [r results]
           (report! r (get-in expected [(:gate r) (:table r)] {}) (get-in here [(:gate r) (:table r)] {})))
         ;; answers whose word changed: refused in the fixture, accepted in the store, or the other way
         (doseq [gate gate-order
                 :let [exp (get-in expected [gate :answers]) got (get-in here [gate :answers])]
                 k (sort (keys exp))
                 :let [a (get exp k) b (get got k)]
                 :when (and (map? a) (map? b) (not= (:refused a) (:refused b)))]
           (let [act-uuid (second (str/split k #" " 2))
                 written (for [table [:facts :facts-by-value]
                               [fk row] (get-in here [gate table])
                               :when (and (map? row) (= act-uuid (str (:by-act row))))]
                           [table fk (contains? (get-in expected [gate table]) fk)])]
             (println "WORD CHANGED" (name gate) "answer" k "| fixture:" (pr-str (:refused a :accepted))
                      "| store:" (pr-str (:refused b :accepted)) "| store answer:" (cut b 300)
                      "| rows the store holds written by that act [table key in-the-fixture?]:" (cut (vec written) 1200))))
         (let [tot (fn [k] (reduce + (map k results)))
               tot-c (fn [k] (reduce + (map (comp count k) results)))]
           (println "TOTAL fixture rows:" (tot :fixture-rows) "| store rows:" (tot :store-rows) "| equal:" (tot :equal)
                    "| same key, other value:" (tot-c :in-place) "| key holds a stamp, paired:" (tot-c :pairs)
                    "| fixture only, unpaired:" (tot-c :fixture-only) "| store only, unpaired:" (tot-c :store-only)
                    "| fixture rows that differ (in place + paired + fixture only):"
                    (+ (tot-c :in-place) (tot-c :pairs) (tot-c :fixture-only))))))))
  (shutdown-agents)
  (System/exit 0))

(try (-main)
     (catch Throwable t
       (println "THREW" (cut (str t) 2000))
       (.printStackTrace t)
       (System/exit 1)))
