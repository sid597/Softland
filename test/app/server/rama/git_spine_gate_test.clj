(ns app.server.rama.git-spine-gate-test
  "REVIEWER-AUTHORED final-phase gate (HQ/Fable, 2026-07-05; WP1-gate
   precedent for test-only blocks at gate).

   G8 PAIR (git-spine CONTRACT v1.2 §3.C + §3.E): the /assert route's
   independently-written plain-map log line must replay through git-spine's
   independently-written reader into a FRESH cluster with the SAME relation
   identity. The route serializer (server_jetty) and the replay reader
   (git_spine) share no code — SF-R2.1 named this exact seam as the drift
   risk, and this test is where the two implementations meet. Same-cluster
   double-replay idempotency is already covered in git_spine_test's replay
   test and is not repeated here."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [app.server-jetty :as sj]
            [app.server.rama.git-spine :as gs]
            [app.server.rama.relation-kernel :as rk]))

(defn- tmp-log-path []
  (str (io/file (str (java.nio.file.Files/createTempDirectory
                      "gs-gate" (make-array java.nio.file.attribute.FileAttribute 0)))
                "relation-assert-log.ednl")))

(deftest g8-pair-route-line-replays-into-fresh-cluster
  (let [log-path (tmp-log-path)
        rt-a (rk/start-relation-runtime! {:tasks 2 :threads 2})]
    (try
      (let [params {:kind :produced
                    :from-kind :conversation
                    :from-id "oc:chat-conversation:chat:g8-gate-sess"
                    :to-kind :container :to-id "oc:doc:g8-gate-commit"
                    :note "gate-g8" :asserter-id "sid" :asserter-type :human}
            resp (sj/assert-relation-handler {:runtime rt-a :log-path log-path} params)
            body (edn/read-string (:body resp))
            rid  (:relation-id body)]
        (is (= 200 (:status resp)))
        (is (str/starts-with? (str rid) "rel:"))
        (is (some? (rk/await-relation #(:row (rk/read-relation-detail rt-a rid)) some?))
            "edge materialized in the ROUTE's own cluster")

        (testing "the route-written line is §3.C-pure (no tags; edn reads it)"
          (let [line (str/trim (slurp log-path))]
            (is (not (str/includes? line "#")) "no reader tags in the log line")
            (is (map? (edn/read-string line)) "clojure.edn round-trips it")))

        (testing "PAIR: fresh cluster + git-spine's reader over the route's line"
          (let [rt-b (rk/start-relation-runtime! {:tasks 2 :threads 2})]
            (try
              (let [{:keys [replayed]} (gs/replay-assert-log!
                                        {:runtime rt-b :assert-log-path log-path})]
                (is (= 1 replayed) "one line, one replay")
                (is (some? (rk/await-relation
                            #(:row (rk/read-relation-detail rt-b rid)) some?))
                    "edge reconstructed in the FRESH cluster")
                (let [{:keys [row history]} (rk/read-relation-detail rt-b rid)]
                  (is (= rid (:relation-id row))
                      "IDENTICAL relation identity across clusters — durability is
                       state reconstruction, not decision-row reuse")
                  (is (= :produced (:relation-kind row)))
                  (is (= 1 (count history)) "exactly one event in the new cluster")))
              (finally (rk/close-relation-runtime! rt-b))))))
      (finally (rk/close-relation-runtime! rt-a)))))
