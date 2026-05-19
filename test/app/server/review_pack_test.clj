(ns app.server.review-pack-test
  (:require
    [clojure.test :refer [deftest is testing use-fixtures]]
    [app.server.review-pack :as rp]))

(use-fixtures
  :each
  (fn [f]
    (reset! rp/!review-packs {})
    (f)))

(defn- valid-pack-request
  []
  {:issue-ref "DG-101"
   :author "sid"
   :branch "feature/review-pack"
   :changed-files [{:path "src/app/server_jetty.clj"
                    :rationale "Expose Review Pack endpoints"
                    :risk-tag :medium}]
   :claims [{:id "c1"
             :text "Review Pack API is exposed via /api/review-pack/*"
             :core? true
             :evidence-ids ["e1"]}]
   :sections {:intent "Add Review Pack v0 API surface."
              :scope "Server-side routes and domain validation."
              :change-summary "Created review pack domain module and wired API endpoints."
              :decisions [{:status :accepted
                           :summary "Use external pack + PR link"
                           :rationale "Bounded PR noise with rich linked context"}]
              :evidence [{:id "e1"
                          :kind :code
                          :file-path "src/app/server_jetty.clj"
                          :commit "abc123"
                          :span {:line-start 452 :line-end 570}
                          :snippet "(defn wrap-file-api ...)"
                          :confidence 0.9}]
              :risks-unknowns [{:kind :risk
                                :severity :medium
                                :text "Need end-to-end API tests in full runtime environment."}]}})

(deftest create-review-pack-success
  (testing "Valid pack request produces draft review pack with required structure"
    (let [res (rp/create-review-pack! (valid-pack-request))
          pack (:pack res)]
      (is (true? (:ok res)))
      (is (string? (:pack-id res)))
      (is (= :draft (:status pack)))
      (is (= 1 (:version pack)))
      (is (= :strict-core-claims (get-in pack [:constraints :evidence-bar])))
      (is (= 1 (count (:claims pack))))
      (is (= 1 (count (get-in pack [:sections :evidence])))))))

(deftest create-review-pack-validation-errors
  (testing "Missing rationale and missing core-claim evidence fails validation"
    (let [res (rp/create-review-pack!
                {:issue-ref "DG-102"
                 :changed-files [{:path "src/app/foo.clj"}]
                 :claims [{:text "Core claim without anchors"
                           :core? true
                           :evidence-ids []}]
                 :sections {:intent "x"
                            :scope "y"
                            :change-summary "z"
                            :decisions []
                            :evidence []
                            :risks-unknowns []}})
          error-types (set (map :type (:errors res)))]
      (is (false? (:ok res)))
      (is (contains? error-types :invalid-changed-file))
      (is (contains? error-types :core-claim-missing-evidence)))))

(deftest publish-and-summary
  (testing "Publishing computes snapshot hash and summary reflects core counters"
    (let [create-res (rp/create-review-pack! (valid-pack-request))
          pack-id (:pack-id create-res)
          publish-res (rp/publish-review-pack! pack-id {:pr-ref "https://github.com/acme/repo/pull/42"
                                                        :published-by "sid"})
          summary-res (rp/review-pack-summary pack-id {:base-url "https://softland.local"})]
      (is (true? (:ok publish-res)))
      (is (string? (:snapshot-sha publish-res)))
      (is (= :published (get-in publish-res [:pack :status])))
      (is (true? (:ok summary-res)))
      (is (= 1 (get-in summary-res [:summary :core-claim-count])))
      (is (= 1 (get-in summary-res [:summary :evidence-anchor-count])))
      (is (re-find #"Review Pack" (:pr-comment-template summary-res))))))
