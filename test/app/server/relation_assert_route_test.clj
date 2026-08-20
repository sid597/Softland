;; git-spine WP2 component W (PW) — /assert write-shim tests (CONTRACT §3.E, the
;; PW half of gate G8). The final-phase G8 PAIR test (spanning P1's
;; replay-assert-log! + PW's assert-relation-handler) lives with the orchestrator;
;; these are PW's own gates, drivable without the git_spine namespace.
;;
;; Seams under test (all public in app.server-jetty):
;;   assert-relation-handler        — validate → write-ahead → depot append → response
;;   resolve-trail-runtime-or-503   — direct external-cluster availability
;;   handle-assert-route            — route composition (resolve → parse → delegate)
;;
;; The handler takes {:runtime :log-path} so tests drive it with a tests-only
;; relation runtime (rk/start-relation-runtime!) + a tmp log path — the real
;; cluster runtime is supplied directly; the 503 test supplies nil.

(ns app.server.relation-assert-route-test
  (:require [app.server-jetty :as sj]
            [app.server.episode :as episode]
            [app.server.rama.cluster :as cluster]
            [app.server.rama.relation-kernel :as rk]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]])
  (:import (java.io ByteArrayInputStream)))

(defn- tmp-log-path
  "A fresh, non-existent tmp path (parent = java.io.tmpdir, which exists). Never
   the repo's data/ dir, so tests never pre-create the tracked write-ahead log."
  []
  (str (System/getProperty "java.io.tmpdir")
       "/softland-assert-route-" (System/currentTimeMillis) "-" (rand-int 1000000) ".ednl"))

(deftest curl-default-form-content-type-preserves-product-edn-body
  (let [body "{:block-id \"waist-close-block\" :text \"cleanup close receipt\" :time-ms 1755640000000 :position {:x 100.0 :y 100.0}}"
        seen (atom nil)]
    (with-redefs [cluster/face-projection-runtime
                  (constantly {:oc-rt :receipt-runtime})
                  episode/append-utterance!
                  (fn [runtime args]
                    (reset! seen [runtime args])
                    {:status :accepted :unit-ids ["du:receipt"] :decision {}})]
      (let [response ((sj/http-middleware)
                      {:request-method :post
                       :uri "/api/episode/block-birth"
                       :headers {"content-type" "application/x-www-form-urlencoded"}
                       :content-type "application/x-www-form-urlencoded"
                       :body (ByteArrayInputStream. (.getBytes body "UTF-8"))})]
        (is (= 200 (:status response)))
        (is (= [:receipt-runtime
                {:text "cleanup close receipt"
                 :turn-id "waist-close-block"
                 :time-ms 1755640000000
                 :position {:x 100.0 :y 100.0}
                 :scene-context nil
                 :conversation-id nil}]
               @seen))))))

;; ═══════════════════════════════════════════════════════════════════════════
;;  G8/PW-1 — valid assert: 200, edge materializes, one tag-free EDN log line.
;; ═══════════════════════════════════════════════════════════════════════════
(deftest valid-assert-writes-log-and-materializes-edge
  (let [runtime  (rk/start-relation-runtime! {:tasks 2 :threads 2})
        log-path (tmp-log-path)]
    (try
      (let [params {:kind :based-on
                    :from-kind :container :from-id "oc:doc:git-spine-valid-from"
                    :to-kind   :container :to-id "oc:doc:git-spine-valid-to"
                    :note "spine-v1|file-write"
                    :asserter-id "import:git-spine" :asserter-type :import}
            resp   (sj/assert-relation-handler {:runtime runtime :log-path log-path} params)
            body   (edn/read-string (:body resp))]

        (testing "200 with {relation-id request-id}"
          (is (= 200 (:status resp)))
          (is (true? (:ok body)))
          (is (str/starts-with? (str (:relation-id body)) "rel:"))
          (is (string? (:request-id body))))

        (testing "edge visible via rk/await-relation"
          (let [detail (rk/await-relation
                         #(rk/read-relation-detail runtime (:relation-id body))
                         #(some? (:row %))
                         5000)]
            (is (some? (:row detail)) "edge row materialized")
            (is (= :based-on (:relation-kind (:row detail))))))

        (testing "assert-log grew by exactly one tag-free line that EDN round-trips, ids intact"
          (is (.exists (io/file log-path)))
          (let [lines (str/split-lines (slurp log-path))]
            (is (= 1 (count lines)) "exactly one appended line")
            (is (not (str/includes? (first lines) "#")) "no reader tags in the line")
            ;; edn/read-string THROWS on any reader tag — a clean parse is the
            ;; tag-free proof; the equalities prove ids/shape survived verbatim.
            (let [parsed (edn/read-string (first lines))]
              (is (= (:relation-id body) (:relation/routing-key parsed)))
              (is (= (:request-id body)  (:request/id parsed)))
              (is (= :relation/assert    (:request/type parsed)))
              (is (map? (:payload parsed))          "payload serialized as a plain map")
              (is (map? (:from (:payload parsed)))  "from serialized as a plain map")
              (is (map? (:to (:payload parsed)))    "to serialized as a plain map")
              (is (= :based-on (:relation-kind (:payload parsed))))
              (is (= "oc:doc:git-spine-valid-from"
                     (:target-id (:from (:payload parsed)))))
              (is (= "oc:doc:git-spine-valid-to"
                     (:target-id (:to (:payload parsed)))))))))
      (finally
        (rk/close-relation-runtime! runtime)
        (io/delete-file log-path true)))))

;; ═══════════════════════════════════════════════════════════════════════════
;;  G8/PW-2 — invalid kind: 4xx, file untouched, depot never reached.
;;  nil runtime: validation short-circuits before any depot use, so a rejected
;;  request can NEVER reach append-relation-request! (no NPE ⇒ no depot append).
;; ═══════════════════════════════════════════════════════════════════════════
(deftest invalid-kind-rejected-no-write
  (let [log-path (tmp-log-path)
        params   {:kind :relates-to               ; NOT in rk/relation-kinds
                  :from-kind :container :from-id "oc:doc:x"
                  :to-kind   :container :to-id "oc:doc:y"
                  :asserter-id "sid" :asserter-type :human}
        resp     (sj/assert-relation-handler {:runtime nil :log-path log-path} params)
        body     (edn/read-string (:body resp))]
    (try
      (is (= 400 (:status resp)))
      (is (false? (:ok body)))
      (is (string? (:reason body)) "a clear reason is returned")
      (is (not (.exists (io/file log-path))) "no log file created for an invalid request")
      (finally (io/delete-file log-path true)))))

;; ═══════════════════════════════════════════════════════════════════════════
;;  G8/PW-3 — blank target: 4xx, file untouched.
;; ═══════════════════════════════════════════════════════════════════════════
(deftest blank-target-rejected-no-write
  (let [log-path (tmp-log-path)
        params   {:kind :based-on
                  :from-kind :container :from-id "oc:doc:x"
                  :to-kind   :container :to-id "   "   ; blank
                  :asserter-id "sid" :asserter-type :human}
        resp     (sj/assert-relation-handler {:runtime nil :log-path log-path} params)
        body     (edn/read-string (:body resp))]
    (try
      (is (= 400 (:status resp)))
      (is (false? (:ok body)))
      (is (not (.exists (io/file log-path))) "no log file created for a blank target")
      (finally (io/delete-file log-path true)))))

;; ═══════════════════════════════════════════════════════════════════════════
;;  Gate-review fixes 1–3 (2026-07-05) — DIFF_FALSIFICATION_R1 should-fixes:
;;  (1) asserter custody validated route-side (the depot rejects nil :actor/id
;;      AFTER a 200 — the route must reject FIRST, and never write-ahead a
;;      poison line that re-fails on every boot replay);
;;  (2) deterministic idempotency key: a curl RETRY converges at the journal
;;      (zero duplicate decision/event/activity rows — trap 4), while an
;;      explicit :idempotency-key override still allows a deliberate re-assert;
;;  (3) :git-commit is OUT of the target allowlist (bare-sha target-keys never
;;      join the rendered commit object — the CONTRACT v1.1 B1 non-join).
;; ═══════════════════════════════════════════════════════════════════════════
(deftest asserter-custody-validated-no-write
  (let [log-path (tmp-log-path)
        base     {:kind :based-on
                  :from-kind :container :from-id "oc:doc:x"
                  :to-kind   :container :to-id "oc:doc:y"}]
    (try
      (testing "missing asserter-id → 400, NO file write, NO depot append"
        (let [resp (sj/assert-relation-handler
                    {:runtime nil :log-path log-path}
                    (assoc base :asserter-type :human))
              body (edn/read-string (:body resp))]
          (is (= 400 (:status resp)))
          (is (str/includes? (:reason body) "asserter-id"))
          (is (not (.exists (io/file log-path))))))
      (testing "blank asserter-id → 400"
        (is (= 400 (:status (sj/assert-relation-handler
                             {:runtime nil :log-path log-path}
                             (assoc base :asserter-id "  " :asserter-type :human))))))
      (testing "non-keyword asserter-type → 400"
        (let [resp (sj/assert-relation-handler
                    {:runtime nil :log-path log-path}
                    (assoc base :asserter-id "sid" :asserter-type "human"))]
          (is (= 400 (:status resp)))
          (is (not (.exists (io/file log-path))))))
      (finally (io/delete-file log-path true)))))

(deftest git-commit-target-kind-rejected
  (let [log-path (tmp-log-path)
        params   {:kind :based-on
                  :from-kind :container :from-id "oc:doc:x"
                  :to-kind   :git-commit :to-id "deadbeefcafe1234"
                  :asserter-id "sid" :asserter-type :human}
        resp     (sj/assert-relation-handler {:runtime nil :log-path log-path} params)
        body     (edn/read-string (:body resp))]
    (try
      (is (= 400 (:status resp)))
      (is (str/includes? (:reason body) "git-commit"))
      (is (not (.exists (io/file log-path)))
          "the dangling-edge kind never reaches file or depot")
      (finally (io/delete-file log-path true)))))

(deftest retry-idempotency-converges-at-the-journal
  (let [runtime  (rk/start-relation-runtime! {:tasks 2 :threads 2})
        log-path (tmp-log-path)
        opts     {:runtime runtime :log-path log-path}
        params   {:kind :produced
                  :from-kind :conversation :from-id "oc:chat-conversation:chat:retry-sess"
                  :to-kind   :container :to-id "oc:doc:retry-target"
                  :note "first attempt"
                  :asserter-id "agent:curl" :asserter-type :agent}]
    (try
      (let [r1   (edn/read-string (:body (sj/assert-relation-handler opts params)))
            _    (rk/await-relation
                  #(rk/read-relation-detail runtime (:relation-id r1))
                  #(some? (:row %)) 5000)
            ;; the naive curl RETRY: byte-identical params, new POST
            r2   (edn/read-string (:body (sj/assert-relation-handler opts params)))
            ;; a DELIBERATE re-assert with an explicit override key; same
            ;; relation-id → same partition → FIFO after the retry, so once
            ;; ITS event lands the retry has provably been processed too
            r3   (edn/read-string (:body (sj/assert-relation-handler
                                          opts (assoc params
                                                      :note "deliberate re-assert"
                                                      :idempotency-key "assert-again:retry-1"))))
            _    (rk/await-relation
                  #(rk/read-relation-detail runtime (:relation-id r1))
                  #(= 2 (count (:history %))) 5000)
            {:keys [history]} (rk/read-relation-detail runtime (:relation-id r1))]
        (is (= (:relation-id r1) (:relation-id r2) (:relation-id r3))
            "one semantic relation across all three POSTs")
        (is (= (:request-id r1) (:request-id r2))
            "retry re-sends the SAME deterministic request/idempotency key")
        (is (not= (:request-id r1) (:request-id r3))
            "the override key is honored for deliberate re-asserts")
        (is (= 2 (count history))
            "exactly TWO events: original + deliberate re-assert — the naive
             retry was journal-dropped (no phantom decision/event/activity rows)")
        (is (= 3 (count (str/split-lines (slurp log-path))))
            "write-ahead logged all three POSTs (replay converges at the journal)"))
      (finally
        (rk/close-relation-runtime! runtime)
        (io/delete-file log-path true)))))

;; ═══════════════════════════════════════════════════════════════════════════
;;  G8/PW-4 — unavailable external-cluster runtime: 503.
;; ═══════════════════════════════════════════════════════════════════════════
(deftest unavailable-runtime-returns-503
  (testing "resolver reports nil as unavailable"
    (is (= :unavailable
           (first (sj/resolve-trail-runtime-or-503 nil)))))
  (testing "route returns 503"
    (let [resp (sj/handle-assert-route {:body nil} nil (tmp-log-path))
          body (edn/read-string (:body resp))]
      (is (= 503 (:status resp)))
      (is (false? (:ok body))))))
