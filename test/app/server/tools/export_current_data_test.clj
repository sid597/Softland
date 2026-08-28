(ns app.server.tools.export-current-data-test
  (:require [app.server.tools.export-current-data :as export]
            [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]))

(defrecord ExampleRow [id text nested])

(deftest record-normalization-is-plain-edn-and-exactly-readable
  (let [row {:archive/value (->ExampleRow "u1" "note\ntext"
                                         {:child (->ExampleRow "u2" "child" nil)})}
        line (export/edn-line row)
        reread (edn/read-string line)]
    (is (= {:archive/value
            {:id "u1" :text "note\ntext"
             :nested {:child {:id "u2" :text "child" :nested nil}}}}
           reread))
    (is (not (re-find #"ExampleRow" line)))))

(deftest non-edn-throwables-are-preserved-as-portable-maps
  (let [line (export/edn-line {:failure (ex-info "boom" {:kind :test})})
        reread (edn/read-string line)]
    (is (= :throwable (get-in reread [:failure :archive/type])))
    (is (= "clojure.lang.ExceptionInfo"
           (get-in reread [:failure :throwable/class])))
    (is (= "boom" (get-in reread [:failure :throwable/message])))
    (is (= {:kind :test} (get-in reread [:failure :throwable/data])))
    (is (vector? (get-in reread [:failure :throwable/stacktrace])))
    (is (not (re-find #"#error" line)))))

(deftest unthawable-values-abort-instead-of-being-archived
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo
       #"unthawable stored value"
       (export/edn-line
        {:value {:nippy/unthawable
                 (ClassNotFoundException. "missing.Record")}}))))

(deftest string-keyed-sorted-maps-do-not-trip-the-unthawable-check
  (let [value (sorted-map "a" 1 "b" 2)]
    (is (= value (edn/read-string (export/edn-line value))))))

(deftest partition-keys-cover-each-task-once
  (let [keys (export/partition-keys export/task-count)]
    (is (= (set (range export/task-count))
           (set (map #(mod (hash %) export/task-count) keys))))
    (is (= export/task-count (count (distinct keys))))))

(deftest output-path-refuses-repository-and-rama-custody
  (testing "protected roots and their descendants"
    (is (false? (export/safe-output-path? "/mnt/data/projects/Softland"
                                         "/mnt/data/projects/Softland/archive")))
    (is (false? (export/safe-output-path? "/mnt/data/projects/Softland"
                                         "/mnt/data/rama/export"))))
  (testing "a unique sibling archive is allowed"
    (is (true? (export/safe-output-path? "/mnt/data/projects/Softland"
                                        "/mnt/data/projects/Softland-archive-test-nonexistent")))))

(deftest inventory-is-complete-and-unambiguous
  (let [specs (vec (export/pstate-specs))]
    (is (= 44 (count specs)))
    (is (= 44 (count (set (map (juxt :module :name) specs)))))
    (is (= #{:flat :nested} (set (map :shape specs))))
    (is (= :plain-map
           (:inner-storage
            (first (filter #(= "$$relation-target-descriptors" (:name %)) specs)))))
    (is (= 25 (count (filter #(= :subindexed (:inner-storage %)) specs))))
    (is (= 5 (count (export/expected-module-names))))))
