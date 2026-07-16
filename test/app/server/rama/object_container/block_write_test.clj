(ns app.server.rama.object-container.block-write-test
  "block-write Lane A gates (CONTRACT §8: G1-G5 IPC half, G9). Exercises the REAL
   object-container stream module through the REAL server entry point
   `app.electric-flow/submit-block-edit!` — no new module/depot/topology (BW-T1).

   Fixture: the checked-in deterministic transcript fixture (reused from
   block-distiller-test) ingested + distilled into a real conversation river page —
   consumer 1's exact material (distiller-minted DerivedUnitRow river blocks). No
   dependency on the real 6.9MB corpus, so the suite is green on any box.

   Barrier discipline (/work-package + /rama): the stream topology's :ack IS the
   barrier — submit-block-edit! appends with :ack, so on return the decision +
   materialized rows are visible. No polling anywhere. Byte-identical (G3) and
   content-unchanged (G4) assertions read the PState PHYSICALLY (foreign-pstate
   direct reads), never the public query surface (BW-T5, implementation-quirks
   'Physical (V1) reads vs public queries')."
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as str]
            [app.electric-flow :as ef]
            [app.server.rama.face-projection :as fp]
            [app.server.rama.object-container :as oc]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.rama.object-container.block-distiller :as bd]
            [app.server.rama.object-container.block-distiller-test :as bdt]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]))

;; --- actors (CONTRACT §3/§6; core/authorized-request? core.clj:343) -----------
(def sid-actor
  {:actor/id "sid" :actor/type :human :actor/capabilities #{:object/edit}})

;; G5: NOT :system (that type is auto-authorized, core.clj:349) AND lacks
;; :object/edit → the ONLY reason to reject is the missing capability.
(def unauthorized-actor
  {:actor/id "intruder" :actor/type :human :actor/capabilities #{}})

;; --- physical PState readers (never the public query surface) ------------------
(defn container-physical [oc-rt container-id]
  (foreign-select-one [(keypath container-id)] (:containers-by-id oc-rt)))

(defn revisions-physical [oc-rt container-id]
  (vec (foreign-select [(keypath container-id) MAP-VALS]
                       (:revision-history-by-container oc-rt))))

(defn revision-count [oc-rt container-id]
  (count (revisions-physical oc-rt container-id)))

(defn edit-env
  "One CONTRACT §3 edit envelope over a served river block. `document-container-id`
   is copied VERBATIM from the served block (PHASE_0 rule 2 → outbox), never
   computed client-side (BW-T7). No :content-hash — the server stamps it."
  [object-key block content {:keys [request-id idempotency-key edit-client-id
                                    edit-seq time-ms actor]}]
  {:request-id      request-id
   :idempotency-key idempotency-key
   :edit-client-id  edit-client-id
   :edit-seq        edit-seq
   :time-ms         time-ms
   :actor           actor
   :target          {:target/kind :derived-unit :target/id (:unit-id block)}
   :payload         {:document-container-id (:document-container-id block)
                     :object-key            object-key
                     :content-text          content}})

(defn event-type-of [oc-rt object-key request-id]
  (get-in (ocr/read-decision oc-rt object-key request-id) [:event-row :event-type]))

(deftest block-write-lane-a-gates
  (let [rt    (bd/start-distiller-runtime!)
        oc-rt (:oc-rt rt)]
    (try
      (bdt/ingest-fixture! oc-rt)
      (let [summary    (bd/distill-conversation! {:oc-rt oc-rt
                                                  :source bdt/fixture-source
                                                  :conversation-id bdt/fixture-conversation-id})
            object-key (:object-key summary)
            page       (bd/river-page {:oc-rt oc-rt :object-key object-key}
                                      bd/max-river-page-size)
            ;; distinct blocks per gate — no cross-gate mutable interference
            [bA bB bC bD] (mapv page [0 1 2 3])]

        (testing "PHASE_0 rule 1/2 · every served block carries its OWN document-container-id"
          (is (seq page))
          (is (every? #(some? (:document-container-id %)) page)
              "rule 1 additive projection field present on every river block")
          ;; provenance: the served value equals the physical DerivedUnitRow field —
          ;; NOT chat-conversation-id (PHASE_0 rejected reading (a)); the map cannot lie
          (is (every? (fn [b]
                        (= (:document-container-id b)
                           (:document-container-id (bdt/read-unit-physical oc-rt (:unit-id b)))))
                      page)
              "served document-container-id = the unit's own DerivedUnitRow field")
          (is (every? #(str/starts-with? (str (:document-container-id %))
                                          "oc:chat-message:")
                      page)
              "consumer-1 river units carry a per-EVENT chat-message-id (PHASE_0)"))

        (testing "G1 round-trip · first edit graduates; read-unit overlays edited content"
          (let [!epoch  (atom 0)
                res     (ef/submit-block-edit!
                         oc-rt
                         (edit-env object-key bA "G1 edited body"
                                   {:request-id "bw-g1" :edit-client-id "sid-c1"
                                    :edit-seq 1 :time-ms 100000 :actor sid-actor})
                         !epoch)
                unit    (ocr/read-unit oc-rt (:unit-id bA))
                cid     (:target-id unit)
                crow    (container-physical oc-rt cid)]
            (is (true? (:accepted? res)))
            (is (false? (:replay? res)))
            (is (= 1 @!epoch) "BW-T9: epoch bumps exactly once in the ack continuation")
            (is (= :object/graduated (event-type-of oc-rt object-key "bw-g1"))
                "first edit of an imported unit = :object/graduated")
            (is (= :object-container (:target-kind unit)))
            (is (= "G1 edited body" (:content-text unit))
                "read-unit returns edited content via the graduation overlay")
            (is (= :text-block (:container-kind crow)))
            ;; map-not-lie end to end: the graduated container's document parent =
            ;; the source unit's own document-container-id (PHASE_0 invariant)
            (is (= (:document-container-id bA) (:document-container-id crow))
                "graduated container document parent = source unit's document-container-id")))

        (testing "G2 revision chain · second edit revises the same container"
          (let [unit0     (ocr/read-unit oc-rt (:unit-id bA))
                cid       (:target-id unit0)
                revs0     (revision-count oc-rt cid)
                !epoch    (atom 0)
                res       (ef/submit-block-edit!
                           oc-rt
                           (edit-env object-key bA "G2 revised body"
                                     {:request-id "bw-g2" :edit-client-id "sid-c1"
                                      :edit-seq 2 :time-ms 100100 :actor sid-actor})
                           !epoch)
                revs1     (revisions-physical oc-rt cid)
                first-rev (first revs1)
                second-rev (second revs1)]
            (is (true? (:accepted? res)))
            (is (= 1 @!epoch))
            (is (= :object/revised (event-type-of oc-rt object-key "bw-g2")))
            (is (= (inc revs0) (count revs1)) "one new revision appended")
            (is (= "G2 revised body" (:current-content-text (container-physical oc-rt cid))))
            (is (= (:revision-id first-rev) (:parent-revision-id second-rev))
                "parent-revision-id links the chain")
            ;; both revisions durable in $$revisions-by-id (physical, keyed by revision-id)
            (is (every? #(some? (foreign-select-one [(keypath (:revision-id %))]
                                                    (:revisions-by-id oc-rt)))
                        revs1)
                "every revision durable in $$revisions-by-id")))

        (testing "§5 narrowing serve · :block-truth returns the NEWEST truth via the registry (INT; F1-fix union-map request)"
          (let [dc (fp/serve {:oc-rt oc-rt}
                             {:face :block-truth :address object-key
                              :params {:units {(:unit-id bA) 1
                                               (:unit-id bC) 2
                                               "oc:unit:nope" 3}}
                              :epoch 3})
                units (:block-truth/units dc)]
            (is (= "G2 revised body" (get-in units [(:unit-id bA) :text]))
                "an edited unit serves the graduation overlay's CURRENT content")
            (is (true? (get-in units [(:unit-id bA) :found?])))
            (is (true? (get-in units [(:unit-id bC) :found?]))
                "a never-edited unit is served too (union map — F1: no unit lost)")
            (is (string? (get-in units [(:unit-id bC) :text])))
            (is (false? (get-in units ["oc:unit:nope" :found?]))
                "unknown unit → honest found? false, no throw")))

        (testing "S2 fix · river-page serves the EDITED content for a graduated block (INT)"
          (let [page (bd/river-page {:oc-rt oc-rt :object-key object-key}
                                    bd/max-river-page-size)
                edited (first (filter #(= (:unit-id bA) (:unit-id %)) page))
                others (remove #(= (:unit-id bA) (:unit-id %)) page)]
            (is (= "G2 revised body" (:text edited))
                "river-page :text = the graduation overlay's current content — edits VISIBLE (G8 survival path)")
            (is (seq others))
            ;; at this point in the gate sequence ONLY bA has been edited
            ;; (G3/G4 edit bD/bB later): every other block's :text must be
            ;; byte-identical to the physical DerivedUnitRow derived text.
            (is (every? (fn [b]
                          (= (:text b)
                             (:derived-content-text
                              (bdt/read-unit-physical oc-rt (:unit-id b)))))
                        (remove #(= (:unit-id bA) (:unit-id %)) page))
                "every never-edited block serves the raw derived text byte-identically")))

        (testing "G3 replay · byte-identical re-append is a journaled no-op (BW-T5)"
          (let [env    (edit-env object-key bD "G3 body"
                                 {:request-id "bw-g3" :idempotency-key "bw-g3-idem"
                                  :edit-client-id "sid-c3" :edit-seq 1
                                  :time-ms 100200 :actor sid-actor})
                !e1    (atom 0)
                r1     (ef/submit-block-edit! oc-rt env !e1)
                unit   (ocr/read-unit oc-rt (:unit-id bD))
                cid    (:target-id unit)
                crow0  (container-physical oc-rt cid)   ;; physical snapshot BEFORE replay
                revs0  (revisions-physical oc-rt cid)
                !e2    (atom 0)
                r2     (ef/submit-block-edit! oc-rt env !e2) ;; identical envelope
                crow1  (container-physical oc-rt cid)   ;; physical snapshot AFTER replay
                revs1  (revisions-physical oc-rt cid)]
            (is (true? (:accepted? r1)))
            (is (false? (:replay? r1)))
            (is (= 1 @!e1))
            (is (true? (:replay? r2)) "the second identical append is a journaled replay")
            (is (= 0 @!e2) "BW-T9/BW-T5: a replay changes no truth → NO epoch bump")
            (is (= crow0 crow1) "container PState byte-identical after replay (physical read)")
            (is (= revs0 revs1) "no second revision written by the replay")))

        (testing "G4 stale ordering · out-of-order edit-seq is a durable :edit/stale"
          (let [!e0  (atom 0)
                _    (ef/submit-block-edit!
                      oc-rt
                      (edit-env object-key bB "G4 seq-5 body"
                                {:request-id "bw-g4-a" :edit-client-id "sid-c4"
                                 :edit-seq 5 :time-ms 100300 :actor sid-actor})
                      !e0)
                unit (ocr/read-unit oc-rt (:unit-id bB))
                cid  (:target-id unit)
                crow0 (container-physical oc-rt cid)
                !e1  (atom 0)
                res  (ef/submit-block-edit!
                      oc-rt
                      ;; seq 3 <= last seq 5, different idempotency → stale (BW-T8 context)
                      (edit-env object-key bB "G4 stale body"
                                {:request-id "bw-g4-b" :edit-client-id "sid-c4"
                                 :edit-seq 3 :time-ms 100400 :actor sid-actor})
                      !e1)
                crow1 (container-physical oc-rt cid)]
            (is (false? (:accepted? res)))
            (is (= :edit/stale (:reason res)) "refusal reason surfaced (G5-shape)")
            (is (= 0 @!e1) "a rejected edit bumps NO epoch")
            (is (= crow0 crow1) "stale edit left container content unchanged (physical read)")))

        (testing "G5 refusal · actor without :object/edit is rejected durably, reason surfaced"
          (let [!e   (atom 0)
                res  (ef/submit-block-edit!
                      oc-rt
                      (edit-env object-key bC "G5 unauthorized body"
                                {:request-id "bw-g5" :edit-client-id "intr-c5"
                                 :edit-seq 1 :time-ms 100500 :actor unauthorized-actor})
                      !e)
                unit (ocr/read-unit oc-rt (:unit-id bC))
                decision (ocr/read-decision oc-rt object-key "bw-g5")]
            (is (false? (:accepted? res)))
            (is (= :request-invalid (:reason res)))
            (is (some #(= :actor-not-authorized (:type %)) (:errors res))
                "the durable decision names the missing capability (map-not-lie)")
            (is (= 0 @!e))
            (is (= :rejected (:status decision)) "durable rejection row")
            ;; read-unit always returns the unit; on a refused edit it must still be
            ;; the UNGRADUATED derived-unit carrying original content (no write leaked)
            (is (= :derived-unit (:target-kind unit)) "target still ungraduated")
            (is (not= "G5 unauthorized body" (:content-text unit)) "no content written")))

        (testing "G9 read-plan conservation · the additive field adds no reads"
          (let [limit bd/max-river-page-size
                p     (bd/river-page {:oc-rt oc-rt :object-key object-key} limit)
                plan  (:river-page/read-plan (meta p))]
            (is (= (+ 1 (* 4 limit)) (:seek-bound plan)) "seek bound stays 1 + 4*limit")
            (is (<= (:seek-count plan) (:seek-bound plan)))
            (is (= (:unit-reads plan) (:blocks-returned plan))
                "no extra point-reads — document-container-id rides the already-read unit")
            (is (every? #(some? (:document-container-id %)) p)
                "rule-1 field still present on the re-pulled page"))))
      (finally (bd/close-distiller-runtime! rt)))))

(deftest g8-block-edit-wal-survives-runtime-restart
  (let [wal (java.io.File/createTempFile "block-edit-" ".ednl")]
    (.delete wal)
    (try
      (let [rt (bd/start-distiller-runtime!)
            oc-rt (assoc (:oc-rt rt) :block-edit-log-path (.getPath wal))]
        (try
          (bdt/ingest-fixture! oc-rt)
          (let [summary (bd/distill-conversation! {:oc-rt oc-rt
                                                   :source bdt/fixture-source
                                                   :conversation-id bdt/fixture-conversation-id})
                object-key (:object-key summary)
                block (first (bd/river-page {:oc-rt oc-rt :object-key object-key}
                                            bd/max-river-page-size))
                res (ef/submit-block-edit!
                     oc-rt
                     (edit-env object-key block "G8 persisted body"
                               {:request-id "bw-g8-restart"
                                :edit-client-id "sid-g8"
                                :edit-seq 1
                                :time-ms 100600
                                :actor sid-actor})
                     (atom 0))]
            (is (true? (:accepted? res)))
            (is (.exists wal) "accepted edit intent is WAL'd before ack"))
          (finally (bd/close-distiller-runtime! rt))))

      (let [rt (bd/start-distiller-runtime!)
            oc-rt (assoc (:oc-rt rt) :block-edit-log-path (.getPath wal))]
        (try
          (bdt/ingest-fixture! oc-rt)
          (let [summary (bd/distill-conversation! {:oc-rt oc-rt
                                                   :source bdt/fixture-source
                                                   :conversation-id bdt/fixture-conversation-id})
                object-key (:object-key summary)
                block (first (bd/river-page {:oc-rt oc-rt :object-key object-key}
                                            bd/max-river-page-size))
                stats (ocr/replay-block-edit-log! oc-rt)
                unit (ocr/read-unit oc-rt (:unit-id block))]
            (is (= {:replayed 1 :failed 0} stats))
            (is (= "G8 persisted body" (:content-text unit))
                "the same unit reads edited truth after a fresh runtime rebuild"))
          (finally (bd/close-distiller-runtime! rt))))
      (finally (.delete wal)))))
