(ns app.server.rama.text-kernel-probe-test
  "Depot-adversary probes for the text kernel (fix session 4).

   One IPC launch, one deftest, sequential testing blocks. Each block asserts
   the SEMANTIC payload the contract promises (decision status + reason, event
   identity, head content, view previews), not just row existence.

   Failing baseline (pre-fix, recorded 2026-06-11):
     dup-diff      → both events existed, decision flipped, head moved
     dup-same      → :decided-at rewritten on replay
     race          → status-set racing its own ingest rejected :target-unit-not-found
     re-attach     → old judgment silently discarded the new revision's text
     garbage       → CallbackException / poison, no durable rejection"
  (:require [app.server.rama.core :as core]
            [app.server.rama.probe-harness :as probe]
            [app.server.rama.text-kernel :as tk]
            [app.server.rama.util-fns :as util-fns]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [com.rpl.rama :refer [foreign-append!]]))

(defn fence!
  "Settle fence on one routing key: append a throwaway status-set for a unit
   that cannot exist and await its (rejected) decision. Same key → same task →
   depot order, so once the fence's decision is visible every record appended
   before it on this key has been consumed. The fence rejects, so it writes no
   world state."
  [runtime artifact-id]
  (let [req (tk/unit-status-request (str artifact-id "/none/line/999") :rejected
                                    {:artifact-id artifact-id})]
    (tk/append-action-request! runtime req)
    (tk/await-decision runtime (:routing/key req) (:request/id req) 5000)))

(defn ingest!
  [runtime artifact-id revision-id content & [opts]]
  (tk/ingest-text! runtime content (merge {:artifact-id artifact-id
                                           :revision-id revision-id}
                                          opts)))

(defn artifact-truth
  "Committed-truth snapshot for unchanged-truth probes on one artifact."
  [runtime routing-key artifact-id request-id]
  {:decision (tk/read-decision runtime routing-key request-id)
   :head (tk/read-text-head runtime artifact-id)
   :artifact (tk/read-artifact runtime artifact-id)})

(deftest text-kernel-adversary-probe-matrix-test
  (let [runtime (tk/start-text-runtime!)]
    (try
      (testing "duplicate request-id, same payload: replay is a total no-op, decided-at stable"
        (let [request (tk/ingest-text-request "same content"
                                              {:request-id "req_dup_same"
                                               :proposed-event-id "evt_dup_same"
                                               :artifact-id "art_dup_same"
                                               :revision-id "rev_dup_same"
                                               :time-ms 10})
              rk (:routing/key request)]
          (tk/append-action-request! runtime request)
          (tk/await-decision runtime rk "req_dup_same")
          (let [result (probe/probe-duplicate-id-same-payload!
                         {:read-state #(artifact-truth runtime rk "art_dup_same" "req_dup_same")
                          :append! #(tk/append-action-request! runtime request)
                          :settle! #(fence! runtime "art_dup_same")})]
            (is (:pass? result) (pr-str result))
            (is (= 10 (get-in result [:after :decision :decided-at]))
                "decided-at comes from the request clock, not the kernel wall clock")
            (is (= "evt_dup_same" (get-in result [:after :decision :event/id]))))
          ;; A client retry that re-mints :request/time-ms is the same intent:
          ;; still a total no-op, decided-at still the first delivery's clock.
          (let [result (probe/probe-duplicate-id-same-payload!
                         {:read-state #(artifact-truth runtime rk "art_dup_same" "req_dup_same")
                          :append! #(tk/append-action-request! runtime (assoc request :request/time-ms 99))
                          :settle! #(fence! runtime "art_dup_same")})]
            (is (:pass? result) (pr-str result))
            (is (= 10 (get-in result [:after :decision :decided-at]))))))

      (testing "duplicate request-id, different payload: conflict is a total no-op, no second event"
        (let [original (tk/ingest-text-request "original"
                                               {:request-id "req_dup_diff"
                                                :proposed-event-id "evt_dup_a"
                                                :artifact-id "art_dup_diff"
                                                :revision-id "rev_a"
                                                :time-ms 11})
              impostor (tk/ingest-text-request "tampered"
                                               {:request-id "req_dup_diff"
                                                :proposed-event-id "evt_dup_b"
                                                :artifact-id "art_dup_diff"
                                                :revision-id "rev_b"
                                                :time-ms 12})
              rk (:routing/key original)]
          (tk/append-action-request! runtime original)
          (tk/await-decision runtime rk "req_dup_diff")
          (let [result (probe/probe-duplicate-id-different-payload!
                         {:read-state #(artifact-truth runtime rk "art_dup_diff" "req_dup_diff")
                          :append! #(tk/append-action-request! runtime impostor)
                          :settle! #(fence! runtime "art_dup_diff")})]
            (is (:pass? result) (pr-str result))
            (is (= "evt_dup_a" (get-in result [:after :decision :event/id])))
            (is (= "original" (get-in result [:after :head :text/content]))
                "the artifact head still reflects the committed ingest")
            (is (nil? (tk/read-event runtime rk "evt_dup_b"))
                "the conflicting payload never became an event")
            (is (= original (tk/read-request runtime rk "req_dup_diff"))
                "the audit request row is the original, not the impostor"))))

      (testing "event-id collision from a different request rejects :event-id-conflict"
        (ingest! runtime "art_coll" "rev_coll_1" "victim"
                 {:request-id "req_coll_1" :proposed-event-id "evt_coll" :time-ms 13})
        (let [thief (tk/ingest-text-request "thief"
                                            {:request-id "req_coll_2"
                                             :proposed-event-id "evt_coll"
                                             :artifact-id "art_coll"
                                             :revision-id "rev_coll_2"
                                             :time-ms 14})
              rk (:routing/key thief)]
          (tk/append-action-request! runtime thief)
          (let [decision (tk/await-decision runtime rk "req_coll_2")
                event (tk/read-event runtime rk "evt_coll")]
            (is (= :rejected (:decision/status decision)))
            (is (= :event-id-conflict (:decision/reason decision)))
            (is (= "victim" (get-in event [:payload :text/content]))
                "the committed KernelEvent is immutable"))))

      (testing "status-set racing its own ingest: depot order on one routing key decides"
        (let [ingest-req (tk/ingest-text-request "l1\nl2"
                                                 {:request-id "req_race_i"
                                                  :artifact-id "art_race"
                                                  :revision-id "rev_race"
                                                  :time-ms 15})
              status-req (tk/unit-status-request "art_race/rev_race/line/1" :rejected
                                                 {:request-id "req_race_s"
                                                  :artifact-id "art_race"
                                                  :time-ms 16})]
          ;; No await between the appends — the status must still observe the
          ;; ingest's units because both records serialize on the same task.
          (tk/append-action-request! runtime ingest-req)
          (tk/append-action-request! runtime status-req)
          (let [decision (tk/await-decision runtime (:routing/key status-req) "req_race_s" 5000)]
            (is (= :accepted (:decision/status decision))
                (pr-str (select-keys decision [:decision/status :decision/reason]))))))

      (testing "garbage: keyword-keyed map without request id gets a durable surrogate rejection"
        (let [garbage {:routing/key [:artifact "art_garbage"] :junk 1}
              audit-id (core/audit-request-id garbage)]
          (is (str/starts-with? audit-id "invalid/"))
          (foreign-append! (:text-requests-depot runtime) garbage :append-ack)
          (fence! runtime "art_garbage")
          (let [decision (tk/read-decision runtime [:artifact "art_garbage"] audit-id)]
            (is (= :rejected (:decision/status decision)))
            (is (= :request-invalid (:decision/reason decision)))
            (is (contains? (set (map :type (:errors decision))) :request/id-invalid))
            (is (= garbage (tk/read-request runtime [:artifact "art_garbage"] audit-id))
                "storable garbage is stored verbatim under the surrogate"))))

      (testing "garbage: non-keyword-keyed map is stored as a bounded preview, never a poison write"
        (let [garbage {"oops" 1 :routing/key [:artifact "art_garbage2"]}
              audit-id (core/audit-request-id garbage)]
          (foreign-append! (:text-requests-depot runtime) garbage :append-ack)
          (fence! runtime "art_garbage2")
          (let [decision (tk/read-decision runtime [:artifact "art_garbage2"] audit-id)
                stored (tk/read-request runtime [:artifact "art_garbage2"] audit-id)]
            (is (= :rejected (:decision/status decision)))
            (is (= :request-unstorable (:dead-letter/reason stored)))
            (is (string? (:dead-letter/record-preview stored))))))

      (testing "garbage: a non-map record does not wedge the topology"
        (foreign-append! (:text-requests-depot runtime) "not-a-request" :append-ack)
        ;; Microbatches commit globally, so any later decision proves the
        ;; non-map's partition processed it without poisoning the batch.
        (let [decision (fence! runtime "art_liveness")]
          (is (= :rejected (:decision/status decision))
              "the kernel still decides requests appended after the non-map")))

      (testing "re-ingest: judgments are revision-scoped and never re-attach"
        (ingest! runtime "art_ri" "rev_1" "alpha\nbeta" {:request-id "req_ri_1" :time-ms 17})
        (let [units (tk/unitize-lines! runtime {:payload {:artifact/id "art_ri"}})
              judged-unit-id (:unit/id (second units))]
          (is (= "art_ri/rev_1/line/2" judged-unit-id) "unit ids carry the revision")
          (tk/set-unit-status! runtime judged-unit-id :rejected
                               {:request-id "req_ri_judge" :artifact-id "art_ri" :time-ms 18})
          (is (= ["beta"] (mapv :preview (tk/read-discarded-view runtime core/default-branch-id "art_ri"))))
          (ingest! runtime "art_ri" "rev_2" "gamma\ndelta" {:request-id "req_ri_2" :time-ms 19})
          (tk/await-materialized #(tk/read-units runtime "art_ri")
                                 #(contains? % "art_ri/rev_2/line/1"))
          (is (= [] (mapv :preview (tk/read-discarded-view runtime core/default-branch-id "art_ri")))
              "the old :rejected judgment must not discard the new revision's text")
          (is (= ["gamma" "delta"]
                 (mapv :preview (tk/read-canonical-view runtime core/default-branch-id "art_ri"))))
          (is (some? (get (tk/read-unit-statuses runtime "art_ri" core/default-branch-id)
                          judged-unit-id))
              "the old judgment remains durable audit state")
          ;; A late judgment on the dead revision's unit must reject, not revive it.
          (let [late (tk/unit-status-request judged-unit-id :accepted
                                             {:request-id "req_ri_late" :artifact-id "art_ri"
                                              :time-ms 20})]
            (tk/append-action-request! runtime late)
            (let [decision (tk/await-decision runtime (:routing/key late) "req_ri_late")]
              (is (= :rejected (:decision/status decision)))
              (is (= :target-unit-not-found (:decision/reason decision)))))))

      (testing "revisions are immutable: re-ingesting an existing revision id rejects"
        (ingest! runtime "art_imm" "rev_imm" "original" {:request-id "req_imm_1" :time-ms 21})
        (let [tamper (tk/ingest-text-request "tampered"
                                             {:request-id "req_imm_2"
                                              :artifact-id "art_imm"
                                              :revision-id "rev_imm"
                                              :time-ms 22})]
          (tk/append-action-request! runtime tamper)
          (let [decision (tk/await-decision runtime (:routing/key tamper) "req_imm_2")]
            (is (= :rejected (:decision/status decision)))
            (is (= :revision-exists (:decision/reason decision)))
            (is (= "original" (:text/content (tk/read-text-head runtime "art_imm")))))))

      (testing "compat is allow-listed: unknown event types cannot mint KernelEvents"
        (let [denied (core/compat-record-request {:event-type :evil/arbitrary-event
                                                  :target-kind :projection
                                                  :target-id "evil"
                                                  :action-type :evil/arbitrary-event
                                                  :payload {:x 1}})]
          (tk/append-action-request! runtime denied)
          (let [decision (tk/await-decision runtime (:routing/key denied) (:request/id denied))]
            (is (= :rejected (:decision/status decision)))
            (is (= :compat-type-not-allowed (:decision/reason decision)))
            (is (nil? (tk/read-event runtime (:routing/key denied)
                                     (:proposed/event-id denied))))))
        (let [allowed (core/compat-record-request {:event-type :sidebar/file-select
                                                   :target-kind :projection
                                                   :target-id "sidebar"
                                                   :action-type :sidebar/file-select
                                                   :payload {:path "src/x.clj" :name "x.clj"}})]
          (tk/append-action-request! runtime allowed)
          (let [decision (tk/await-decision runtime (:routing/key allowed) (:request/id allowed))]
            (is (= :accepted (:decision/status decision)))
            (is (= (:proposed/event-id allowed) (:event/id decision))
                "the accepted compat event carries the header-proposed identity"))))

      (finally
        (tk/close-text-runtime! runtime)))))

(deftest dedup-gate-classifies-reminted-time-as-replay-test
  (testing "the gate fingerprints the normalized view, so re-minted time-ms is a replay, not a conflict"
    (let [request (tk/ingest-text-request "content" {:request-id "req_fp"
                                                     :artifact-id "art_fp"
                                                     :revision-id "rev_fp"
                                                     :time-ms 1})
          stored (-> (core/accepted-decision request {:event/id "evt_fp"})
                     (core/with-request-fingerprint (tk/dedup-request-view request)))
          retry (assoc request :request/time-ms 999)
          gate (core/decision-dedup-gate stored (tk/dedup-request-view retry))]
      (is (= :replay (:gate/status gate)))
      (is (= :conflict
             (:gate/status (core/decision-dedup-gate
                             stored
                             (tk/dedup-request-view (assoc-in retry [:payload :text/content] "other")))))
          "different content under the same id is still a conflict"))))

(deftest mirror-quarantine-covers-every-atom-test
  (testing "the util-fns mirror atoms are declared out of the kernel contract"
    (let [quarantine util-fns/transitional-mirror-quarantine]
      (is (false? (:kernel-contract? quarantine)))
      (is (false? (:durable? quarantine)))
      (is (true? (:reset-on-restart? quarantine)))
      (is (= :none (:rebuild-path quarantine)))))
  (testing "every mirror atom in the namespace is covered by the declaration"
    (let [declared (set (map name (:mirrors util-fns/transitional-mirror-quarantine)))
          actual (->> (ns-publics 'app.server.rama.util-fns)
                      (keep (fn [[sym v]]
                              (when (and (str/starts-with? (name sym) "!")
                                         (instance? clojure.lang.Atom (var-get v)))
                                (name sym))))
                      set)]
      (is (= declared actual)
          "an undeclared mirror atom (or a stale declaration) fails this test"))))
