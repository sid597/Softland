(ns app.client.workspace.selection-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.workspace.selection :as selection]))

(def effective
  {1 {:affine [2.0 0.0 0.0 2.0 100.0 40.0] :flags 0}
   2 {:affine [1.0 0.0 0.0 1.0 0.0 0.0] :flags 0}})

(def targets
  {:block [{:vi :vi/block :container 2
            :bounds {:x 10.0 :y 10.0 :w 20.0 :h 20.0}
            :family :render.family/rect}]
   :fixture [{:vi :vi/fixture :container 1
              :bounds {:x 5.0 :y 5.0 :w 10.0 :h 10.0}
              :family :render.family/path}]
   :edge [{:vi :vi/edge :container 2
           :bounds {:x 0.0 :y 0.0 :w 500.0 :h 500.0}
           :family :render.family/connector}]
   :chrome [{:vi :vi/chrome :container 2
             :bounds {:x 0.0 :y 0.0 :w 500.0 :h 500.0}
             :family :render.family/chrome}]})

(deftest selection-truth-and-clear-tripwire
  (let [block {:vi :vi/block :address :block}
        fixture {:vi :vi/fixture :address :fixture}
        edge {:vi :vi/edge :address :edge}
        s0 (selection/empty-selection)
        s1 (selection/toggle s0 block)
        s2 (selection/toggle s1 fixture)
        s3 (selection/toggle s2 block)
        s4 (selection/marquee-commit s3 #{block fixture})
        fixture-only (selection/marquee-commit s4 #{fixture})
        edge-only (selection/toggle (selection/clear fixture-only) edge)]
    (is (= #{block fixture} (:members s2)))
    (is (= (get-in s1 [:member-revisions block])
           (get-in s2 [:member-revisions block]))
        "adding one member does not invalidate an existing member's chrome")
    (is (not= (get-in s2 [:member-revisions block])
              (get-in s2 [:member-revisions fixture])))
    (is (= #{fixture} (:members s3)))
    (is (= #{block fixture} (:members s4)) "fresh marquee replaces")
    (is (= #{} (selection/legacy-projection fixture-only #{:block})))
    (is (seq (:members fixture-only))
        "fixture-only truth survives even when the legacy projection is empty")
    (is (= #{edge} (:members edge-only))
        "a shift-clickable connector remains selection truth without chrome")
    (is (= {:selected 1 :pruned 0 :blocks 0 :fixtures 0 :edges 1}
           (selection/selection-census edge-only targets))
        "the connector's deliberately invisible v1 selection stays inspectable")
    (is (empty? (:members (selection/clear fixture-only))))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"requires non-nil"
                          (selection/toggle s0 {:vi nil :address :bad})))))

(deftest marquee-index-transform-exclusion-and-prune-tripwire
  (let [sweep (selection/marquee-rect [108.0 48.0] [132.0 72.0])
        hits (selection/marquee-hits sweep targets effective)
        expected #{{:vi :vi/fixture :address :fixture}}
        selected (selection/marquee-commit (selection/empty-selection)
                                           (conj hits {:vi :gone :address :gone}))
        pruned (selection/frame-prune selected
                                      (selection/live-identities targets))]
    (is (= {:x 108.0 :y 48.0 :w 24.0 :h 24.0} sweep))
    (is (= expected hits)
        "non-identity local-to-world composition hits fixture and excludes edge/chrome")
    (is (= expected (:members pruned)))
    (is (= 1 (:last-pruned pruned)))
    (is (= 1 (:selected (selection/selection-census pruned targets))))
    (testing "no naked-drag transition exists"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Unknown selection"
                            (selection/transition pruned {:op :naked-drag}))))))
